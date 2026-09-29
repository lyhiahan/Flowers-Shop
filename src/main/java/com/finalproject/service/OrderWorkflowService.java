package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final StoreRepository stores;
    private final StoreInventoryRepository inventories;
    private final TransactionRepository transactions;
    private final StoreAccessService access;
    private final jakarta.persistence.EntityManager entityManager;

    public Order lockedOrder(Long id) {
        return orders.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
    }

    @Transactional
    public Order transition(Long id, OrderStatus next) {
        Order order = lockedOrder(id);
        User actor = access.currentUser();
        if (actor.getRole() == Role.SHIPPER) {
            if (order.getShipper() == null || !actor.getId().equals(order.getShipper().getId())
                    || (next != OrderStatus.DELIVERING && next != OrderStatus.DELIVERED)) {
                throw new AccessDeniedException("Bạn chỉ được cập nhật giao hàng cho đơn được phân công.");
            }
        } else {
            access.requireStore(order.getPickupStore());
            if (next == OrderStatus.REFUNDED) access.requireManager(order.getPickupStore());
        }
        OrderStatus previous = order.getStatus();
        if (previous == next) return order;
        boolean pickup = "PICKUP".equals(order.getShippingMethod());
        boolean allowed = switch (previous) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.CANCELLED
                    || (pickup && next == OrderStatus.COMPLETED)
                    || (!pickup && next == OrderStatus.PICKED_UP && order.getShipper() != null);
            case PICKED_UP -> next == OrderStatus.DELIVERING;
            case DELIVERING -> next == OrderStatus.DELIVERED;
            case DELIVERED -> next == OrderStatus.COMPLETED;
            case REFUND_REQUESTED -> next == OrderStatus.REFUNDED;
            default -> false;
        };
        if (!allowed) throw new IllegalStateException("Không thể chuyển đơn từ " + previous + " sang " + next);
        if (next == OrderStatus.CANCELLED) return cancel(order);
        if (next == OrderStatus.REFUNDED) refund(order);
        if ((next == OrderStatus.DELIVERED || next == OrderStatus.COMPLETED) && previous != OrderStatus.DELIVERED) {
            if ("COD".equals(order.getPaymentMethod()) || "CASH".equals(order.getPaymentMethod())) {
                markPaid(order);
            } else if (!"PAID".equals(order.getPaymentStatus())) {
                throw new IllegalStateException("Cần xác nhận đã nhận thanh toán trước khi hoàn tất đơn.");
            }
            creditIncome(order);
        }
        order.setStatus(next);
        return orders.save(order);
    }

    @Transactional
    public void confirmPayment(Long id) {
        Order order = lockedOrder(id);
        access.requireManager(order.getPickupStore());
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED
                || order.getStatus() == OrderStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("Đơn đã hủy hoặc đang hoàn tiền.");
        }
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod())) {
            throw new IllegalStateException("Chỉ xác nhận thủ công cho chuyển khoản ngân hàng.");
        }
        markPaid(order);
        orders.save(order);
    }

    private void markPaid(Order order) {
        if ("PAID".equals(order.getPaymentStatus())) return;
        var records = payments.findByOrderId(order.getId());
        if (records.size() != 1 || records.get(0).getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Bản ghi thanh toán không hợp lệ, cần đối soát.");
        }
        Payment payment = records.get(0);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payments.save(payment);
        order.setPaymentStatus("PAID");
    }

    private void creditIncome(Order order) {
        Store store = stores.findLockedById(order.getPickupStore().getId()).orElseThrow();
        entityManager.refresh(store, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT").isEmpty()) return;
        BigDecimal rate = store.getCommission() == null ? BigDecimal.TEN : store.getCommission().getCost();
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException("Tỷ lệ hoa hồng phải nằm trong khoảng 0 đến 100%.");
        }
        BigDecimal income = order.getTotalAmount().multiply(BigDecimal.ONE.subtract(rate.movePointLeft(2)))
                .setScale(2, RoundingMode.HALF_UP);
        store.setEWallet(store.getEWallet().add(income));
        stores.save(store);
        transactions.save(Transaction.builder().order(order).store(store).amount(income).isUp(true)
                .type("ORDER_PAYMENT").status("COMPLETED")
                .description("Thu nhập đơn #" + order.getId() + " sau hoa hồng " + rate + "%").build());
    }

    @Transactional
    public void cancelByCustomer(String username, Long id) {
        Order order = lockedOrder(id);
        requireCustomer(order, username);
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        if (order.getStatus() != OrderStatus.PENDING) throw new IllegalStateException("Chỉ hủy được đơn chờ xác nhận.");
        cancel(order);
    }

    private Order cancel(Order order) {
        for (OrderDetail detail : order.getOrderDetails()) {
            StoreInventory inventory = inventories.findByStoreIdAndProductId(order.getPickupStore().getId(), detail.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy tồn kho của đơn hàng."));
            inventory.setQuantity(inventory.getQuantity() + detail.getQuantity());
            inventories.save(inventory);
        }
        boolean paid = "PAID".equals(order.getPaymentStatus());
        order.setStatus(paid ? OrderStatus.REFUND_REQUESTED : OrderStatus.CANCELLED);
        if (!paid) {
            payments.findByOrderId(order.getId()).forEach(payment -> {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payments.save(payment);
                }
            });
        }
        return orders.save(order);
    }

    @Transactional
    public void requestRefund(String username, Long id) {
        Order order = lockedOrder(id);
        requireCustomer(order, username);
        if (order.getStatus() == OrderStatus.REFUND_REQUESTED) return;
        if ((order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED)
                || !"PAID".equals(order.getPaymentStatus())) {
            throw new IllegalStateException("Chỉ yêu cầu hoàn tiền cho đơn đã giao và đã thanh toán.");
        }
        order.setStatus(OrderStatus.REFUND_REQUESTED);
        orders.save(order);
    }

    private void requireCustomer(Order order, String username) {
        User actor = access.currentUser();
        if (!actor.getUsername().equals(username) || !order.getUser().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Đơn hàng không thuộc tài khoản của bạn.");
        }
    }

    // Called only after a manager confirms that the manual refund has actually been paid.
    private void refund(Order order) {
        if (!"PAID".equals(order.getPaymentStatus())) throw new IllegalStateException("Đơn chưa được thanh toán.");
        Store store = stores.findLockedById(order.getPickupStore().getId()).orElseThrow();
        entityManager.refresh(store, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT").forEach(income -> {
            store.setEWallet(store.getEWallet().subtract(income.getAmount()));
            stores.save(store);
            transactions.save(Transaction.builder().order(order).store(store).amount(income.getAmount()).isUp(false)
                    .type("REFUND").status("COMPLETED").description("Hoàn thu nhập đơn #" + order.getId()).build());
        });
        var records = payments.findByOrderId(order.getId());
        if (records.stream().noneMatch(p -> p.getStatus() == PaymentStatus.SUCCESS)) {
            throw new IllegalStateException("Không tìm thấy thanh toán thành công để hoàn tiền.");
        }
        records.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).forEach(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            payment.setRefundMethod("MANUAL");
            payment.setRefundNote("Quản lý xác nhận đã hoàn tiền cho khách");
            payments.save(payment);
        });
        order.setPaymentStatus("REFUNDED");
    }
}
