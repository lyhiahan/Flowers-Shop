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
    private final java.time.Clock clock;
    private final NotificationService notificationService;
    private final OrderFundsService funds;

    public Order lockedOrder(Long id) {
        return orders.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
    }

    public java.util.List<OrderStatus> availableTransitions(Order order) {
        if (order == null || order.getStatus() == null) return java.util.List.of();
        boolean pickup = "PICKUP".equals(order.getShippingMethod());
        return switch (order.getStatus()) {
            case PENDING -> java.util.List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
            case CONFIRMED -> java.util.List.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING -> pickup ? java.util.List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
                    : java.util.List.of(OrderStatus.DELIVERING, OrderStatus.CANCELLED);
            case PICKED_UP -> java.util.List.of(OrderStatus.DELIVERING);
            case DELIVERING -> java.util.List.of(OrderStatus.DELIVERED);
            case DELIVERED -> java.util.List.of(OrderStatus.COMPLETED);
            case REFUND_REQUESTED -> java.util.List.of(OrderStatus.REFUNDED);
            default -> java.util.List.of();
        };
    }

    public String transitionLabel(Order order, OrderStatus next) {
        return switch (next) {
            case CONFIRMED -> "Xác nhận đơn hàng (khách vẫn được hủy)";
            case PREPARING -> "Bắt đầu cắm hoa & Chuẩn bị";
            case PICKED_UP -> "Bàn giao cho người giao hàng";
            case DELIVERING -> "Đang giao hoa đến khách";
            case DELIVERED -> "Đã giao hoa thành công";
            case COMPLETED -> "PICKUP".equals(order.getShippingMethod()) ? "Khách nhận hoa tại quầy - Hoàn tất" : "Hoàn tất đơn hàng";
            case CANCELLED -> order.getStatus() == OrderStatus.PREPARING
                    ? "Hủy ngoại lệ từ cửa hàng (khách không được tự hủy)" : "Hủy đơn hàng";
            case REFUNDED -> "Xác nhận đã hoàn tiền cho khách";
            default -> next.name();
        };
    }

    @Transactional
    public Order transition(Long id, OrderStatus next) {
        return transition(id, next, null);
    }

    @Transactional
    public Order transition(Long id, OrderStatus next, String cancellationReason) {
        Order order = lockedOrder(id);
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        access.requireStore(order.getPickupStore());
        if (next == OrderStatus.REFUNDED) access.requireManager(order.getPickupStore());
        OrderStatus previous = order.getStatus();
        if (previous == next) return order;
        if ("PICKUP".equals(order.getShippingMethod()) && (next == OrderStatus.PICKED_UP || next == OrderStatus.DELIVERING || next == OrderStatus.DELIVERED))
            throw new IllegalStateException("Đơn khách nhận tại tiệm không có bước vận chuyển. Khi khách đã nhận hoa, chọn 'Khách nhận hoa tại quầy - Hoàn tất'.");
        boolean allowed = availableTransitions(order).contains(next);
        if (!allowed) throw new IllegalStateException("Không thể chuyển đơn từ " + previous + " sang " + next);
        if (next == OrderStatus.DELIVERING && !"COD".equals(order.getPaymentMethod())
                && !"CASH".equals(order.getPaymentMethod()) && !"PAID".equals(order.getPaymentStatus())) {
            throw new IllegalStateException("Cần xác nhận đã nhận thanh toán trước khi giao hoa.");
        }
        if (next == OrderStatus.CANCELLED) {
            String reason = cancellationReason == null ? "" : cancellationReason.strip();
            if (reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn hàng.");
            if (reason.length() > 300) throw new IllegalArgumentException("Lý do hủy đơn không được vượt quá 300 ký tự.");
            String actor = access.currentUser().hasRole(Role.ADMIN) ? "admin (quản trị viên)" : "quản lý cửa hàng";
            order.setCancellationReason("Đơn hàng của bạn đã bị hủy bởi " + actor + " với lý do: " + reason + "."
                    + ("PAID".equals(order.getPaymentStatus()) ? " Vui lòng hoàn tiền lại cho khách hàng; chưa ghi nhận đã hoàn tiền." : ""));
            Order saved = cancel(order);
            notifyOrderStatusChange(saved, previous, saved.getStatus());
            return saved;
        }
        if (next == OrderStatus.REFUNDED) {
            refund(order);
            if (order.getCancellationReason() != null) {
                String reason = order.getCancellationReason();
                int refundInstruction = reason.lastIndexOf("Vui lòng hoàn tiền");
                order.setCancellationReason((refundInstruction >= 0 ? reason.substring(0, refundInstruction) : reason + " ")
                        + "Đã xác nhận hoàn tiền cho khách hàng.");
            }
        }
        if ((next == OrderStatus.DELIVERED || next == OrderStatus.COMPLETED) && previous != OrderStatus.DELIVERED) {
            if ("COD".equals(order.getPaymentMethod()) || "CASH".equals(order.getPaymentMethod())) {
                markPaid(order);
            } else if (!"PAID".equals(order.getPaymentStatus())) {
                throw new IllegalStateException("Cần xác nhận đã nhận thanh toán trước khi hoàn tất đơn.");
            }
            creditIncome(order);
        }
        order.setStatus(next);
        Order saved = orders.save(order);
        notifyOrderStatusChange(saved, previous, next);
        return saved;
    }

    @Transactional
    public void confirmPayment(Long id) {
        Order order = lockedOrder(id);
        access.requireManager(order.getPickupStore());
        expireUnpaid(id, LocalDateTime.now(clock));
        if (order.getStatus() == OrderStatus.CANCELLED && Boolean.TRUE.equals(order.getPaymentExpired())) {
            if (!"BANK_TRANSFER".equals(order.getPaymentMethod())) throw new IllegalStateException("Chỉ xác nhận tiền chuyển khoản.");
            markPaid(order);
            order.setStatus(OrderStatus.REFUND_REQUESTED);
            order.setCancellationReason("Tiền chuyển khoản đến sau khi đơn hết hạn. Vui lòng hoàn tiền cho khách; không tiếp tục giao hàng.");
            orders.save(order);
            notifyOrderStatusChange(order, OrderStatus.CANCELLED, OrderStatus.REFUND_REQUESTED);
            return;
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED
                || order.getStatus() == OrderStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("Đơn đã hủy hoặc đang hoàn tiền.");
        }
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod())) {
            throw new IllegalStateException("Chỉ xác nhận thủ công cho chuyển khoản ngân hàng.");
        }
        if ("PAID".equals(order.getPaymentStatus())) return;
        markPaid(order);
        orders.save(order);
        try {
            notificationService.sendNotification(order.getUser(),
                    "💳 Đã nhận tiền chuyển khoản!",
                    "Chi nhánh đã xác nhận nhận đủ tiền chuyển khoản cho đơn hoa #" + order.getId() + ".",
                    "PAYMENT_SUCCESS", order.getId());
        } catch (Exception ignored) {}
    }

    private void markPaid(Order order) {
        if ("PAID".equals(order.getPaymentStatus())) return;
        var records = payments.findByOrderId(order.getId());
        Payment payment;
        if (records.size() > 1) throw new IllegalStateException("Có nhiều bản ghi thanh toán cho đơn hàng.");
        if (!records.isEmpty()) {
            payment = records.get(0);
            if (payment.getAmount() == null || payment.getAmount().compareTo(order.getTotalAmount()) != 0
                    || !(payment.getMethod().name().equals(order.getPaymentMethod())
                        || payment.getMethod() == PaymentMethod.COD && "CASH".equals(order.getPaymentMethod())))
                throw new IllegalStateException("Phương thức hoặc số tiền thanh toán không khớp đơn hàng.");
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now(clock));
        } else {
            payment = Payment.builder()
                    .order(order)
                    .method("MOMO".equalsIgnoreCase(order.getPaymentMethod()) ? PaymentMethod.MOMO : 
                            ("BANK_TRANSFER".equalsIgnoreCase(order.getPaymentMethod()) ? PaymentMethod.BANK_TRANSFER : PaymentMethod.COD))
                    .amount(order.getTotalAmount())
                    .status(PaymentStatus.SUCCESS)
                    .paidAt(LocalDateTime.now(clock))
                    .build();
        }
        payments.save(payment);
        order.setPaymentStatus("PAID");
    }

    private void creditIncome(Order order) {
        Store store = stores.findLockedById(order.getPickupStore().getId()).orElseThrow();
        entityManager.refresh(store, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT").isEmpty()) {
            if ("MOMO_DEMO".equals(order.getPaymentMethod()) && order.getFundsStatus() != OrderFundsStatus.SETTLED)
                throw new IllegalStateException("Thu nhập của đơn không khớp trạng thái tiền, cần đối soát.");
            return;
        }

        BigDecimal rate = "MOMO_DEMO".equals(order.getPaymentMethod()) ? order.getCommissionRateSnapshot()
                : store.getCommission() == null ? BigDecimal.TEN : store.getCommission().getCost();
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException("Tỷ lệ hoa hồng phải nằm trong khoảng 0 đến 100%.");
        }
        BigDecimal income = "MOMO_DEMO".equals(order.getPaymentMethod()) ? funds.settleHeld(order, store)
                : order.getTotalAmount().multiply(BigDecimal.ONE.subtract(rate.movePointLeft(2)))
                    .setScale(2, RoundingMode.HALF_UP);
        store.setEWallet(store.getEWallet().add(income));

        // Tích lũy điểm thi đua cho chi nhánh (1 điểm = 10.000 ₫ doanh số hoàn tất)
        int earnedPoints = order.getTotalAmount().divide(BigDecimal.valueOf(10000), 0, RoundingMode.FLOOR).intValueExact();
        if (earnedPoints > 0) {
            int currentPoint = (store.getPoint() != null) ? store.getPoint() : 0;
            store.setPoint(Math.addExact(currentPoint, earnedPoints));
        }
        stores.save(store);

        String levelName = (store.getStoreLevel() != null && store.getStoreLevel().getName() != null) 
                ? store.getStoreLevel().getName() : "Tiêu chuẩn";
        String desc = "Doanh thu đơn hàng #" + order.getId() + " (+ " + String.format("%,d", earnedPoints) + " điểm thi đua [Hạng " + levelName + "])";

        transactions.save(Transaction.builder().order(order).store(store).amount(income).isUp(true)
                .type("ORDER_PAYMENT").status("COMPLETED")
                .earnedPoints(earnedPoints)
                .description(desc).build());
    }

    @Transactional
    public Order cancelByCustomer(String username, Long id) {
        Order order = lockedOrder(id);
        // Re-read after acquiring the lock: preparation may have started since the page loaded.
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        requireCustomer(order, username);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUND_REQUESTED
                || (order.getStatus() == OrderStatus.REFUNDED && "MOMO_DEMO".equals(order.getPaymentMethod())
                    && "REFUNDED".equals(order.getPaymentStatus()))) return order;
        if (!order.isCancellable())
            throw new IllegalStateException("Không thể hủy đơn đã bắt đầu cắm hoa/chuẩn bị, đang giao hoặc đã kết thúc. Bạn chỉ được hủy khi đơn đang chờ xác nhận hoặc đã xác nhận.");
        boolean automaticRefund = "MOMO_DEMO".equals(order.getPaymentMethod()) && "PAID".equals(order.getPaymentStatus());
        if (automaticRefund) {
            var records = payments.findByOrderId(order.getId());
            if (records.size() != 1) throw new IllegalStateException("Không tìm thấy thanh toán MoMo hợp lệ để hoàn tiền.");
            Payment receipt = records.get(0);
            if (receipt.getMethod() != PaymentMethod.MOMO_DEMO || receipt.getStatus() != PaymentStatus.SUCCESS
                    || receipt.getAmount() == null || order.getTotalAmount() == null
                    || receipt.getAmount().compareTo(order.getTotalAmount()) != 0
                    || !"demo".equals(receipt.getMomoEnvironment())
                    || !("DEMO-MOMO-" + order.getId()).equals(receipt.getTransactionCode()))
                throw new IllegalStateException("Thanh toán MoMo không khớp đơn hàng để hoàn tiền.");
        }
        OrderStatus previous = order.getStatus();
        order.setCancellationReason("Khách hàng đã hủy đơn trước khi bắt đầu cắm hoa/chuẩn bị."
                + ("PAID".equals(order.getPaymentStatus())
                ? " Vui lòng hoàn tiền lại cho khách hàng; chưa ghi nhận đã hoàn tiền."
                : ""));
        cancel(order);
        if (automaticRefund) {
            refund(order, "DEMO_AUTO", "Tự động hoàn tiền demo khi khách hủy đơn MoMo");
            order.setStatus(OrderStatus.REFUNDED);
            order.setCancellationReason("Khách hàng đã hủy đơn trước khi bắt đầu cắm hoa/chuẩn bị. Đã hủy đơn và hoàn tiền thành công.");
            notifyOrderStatusChange(order, previous, OrderStatus.REFUNDED);
        }
        return orders.save(order);
    }

    private Order cancel(Order order) {
        java.util.Map<Long, StoreInventory> stock = new java.util.HashMap<>();
        for (Long productId : order.getOrderDetails().stream().map(d -> d.getProduct().getId()).distinct().sorted().toList()) {
            StoreInventory row = inventories.findLockedStock(order.getPickupStore().getId(), productId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy tồn kho của đơn hàng."));
            entityManager.refresh(row, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            stock.put(productId, row);
        }
        for (OrderDetail detail : order.getOrderDetails()) {
            StoreInventory inventory = stock.get(detail.getProduct().getId());
            inventory.setQuantity(Math.addExact(inventory.getQuantity(), detail.getQuantity()));
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
    public boolean expireUnpaid(Long id, LocalDateTime now) {
        Order order = lockedOrder(id);
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!"UNPAID".equals(order.getPaymentStatus()) || order.getPaymentExpiresAt() == null
                || order.getPaymentExpiresAt().isAfter(now) || (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED)) return false;
        if (!"MOMO".equals(order.getPaymentMethod()) && !"MOMO_DEMO".equals(order.getPaymentMethod())
                && !"BANK_TRANSFER".equals(order.getPaymentMethod())) return false;
        order.setPaymentExpired(true);
        order.setCancellationReason("Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.");
        cancel(order);
        notifyOrderStatusChange(order, OrderStatus.PENDING, OrderStatus.CANCELLED);
        return true;
    }

    @Transactional
    public Order cancelForUnavailableCustomer(Long id, String accountAction) {
        if (!access.currentUser().hasRole(Role.ADMIN)) throw new AccessDeniedException("Chỉ quản trị viên được xử lý tài khoản.");
        Order order = lockedOrder(id);
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.PREPARING) return order;
        if (Boolean.TRUE.equals(order.getUser().getIsActive())) throw new IllegalStateException("Khách hàng vẫn đang hoạt động.");
        boolean paid = "PAID".equals(order.getPaymentStatus());
        String reason = "Khách hàng đã bị " + accountAction + ". Đơn hàng không thành công. "
                + (paid ? "Vui lòng hoàn tiền lại cho khách hàng; chưa ghi nhận đã hoàn tiền." : "Đơn đã tự động hủy vì chưa bàn giao hoa.");
        order.setCancellationReason(reason);
        Order saved = cancel(order);
        notificationService.sendNotificationToStoreManagers(order.getPickupStore().getId(),
                paid ? "Đơn cần hoàn tiền" : "Đơn tự động hủy", reason, "ORDER_UPDATE", order.getId());
        return saved;
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
        refund(order, "MANUAL", "Quản lý xác nhận đã hoàn tiền cho khách");
    }

    private void refund(Order order, String method, String note) {
        if (!"PAID".equals(order.getPaymentStatus())) throw new IllegalStateException("Đơn chưa được thanh toán.");
        Store store = stores.findLockedById(order.getPickupStore().getId()).orElseThrow();
        entityManager.refresh(store, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        boolean refundedFromPending = funds.refundHeld(order, store);
        if (!refundedFromPending) transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT").forEach(income -> {
            store.setEWallet(store.getEWallet().subtract(income.getAmount()));
            int reversedPoints = income.getEarnedPoints() != null ? income.getEarnedPoints()
                    : order.getTotalAmount().divide(BigDecimal.valueOf(10000), 0, RoundingMode.FLOOR).intValueExact();
            store.setPoint(Math.max(0, (store.getPoint() == null ? 0 : store.getPoint()) - reversedPoints));
            stores.save(store);
            transactions.save(Transaction.builder().order(order).store(store).amount(income.getAmount()).isUp(false)
                    .type("REFUND").status("COMPLETED").description("Hoàn thu nhập đơn #" + order.getId()).build());
        });
        if ("MOMO_DEMO".equals(order.getPaymentMethod()) && !refundedFromPending) {
            funds.reverseCommission(order);
            order.setFundsStatus(OrderFundsStatus.REFUNDED);
        }
        var records = payments.findByOrderId(order.getId());
        if (records.stream().noneMatch(p -> p.getStatus() == PaymentStatus.SUCCESS)) {
            throw new IllegalStateException("Không tìm thấy thanh toán thành công để hoàn tiền.");
        }
        records.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).forEach(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now(clock));
            payment.setRefundMethod(method);
            payment.setRefundNote(note);
            payments.save(payment);
        });
        order.setPaymentStatus("REFUNDED");
    }

    private void notifyOrderStatusChange(Order order, OrderStatus prev, OrderStatus next) {
        if (order == null || order.getUser() == null || next == null) return;
        try {
            Long orderId = order.getId();
            switch (next) {
                case CONFIRMED -> {
                    notificationService.sendNotification(order.getUser(),
                            "✅ Đơn hoa #" + orderId + " đã được xác nhận",
                            "Chi nhánh " + (order.getPickupStore() != null ? order.getPickupStore().getName() : "") + " đã tiếp nhận đơn của bạn.",
                            "ORDER_CONFIRMED", orderId);
                }
                case PREPARING -> {
                    notificationService.sendNotification(order.getUser(),
                            "🌺 Đang cắm hoa đơn #" + orderId,
                            "Nghệ nhân tiệm hoa đã bắt đầu cắm hoa/chuẩn bị cho bạn. Từ thời điểm này, bạn không thể tự hủy đơn.",
                            "ORDER_PREPARING", orderId);
                }
                case DELIVERING -> {
                    notificationService.sendNotification(order.getUser(),
                            "🚀 Đơn hoa #" + orderId + " đang được giao",
                            "Đơn hoa của bạn đang trên đường vận chuyển. Vui lòng để ý điện thoại nhé!",
                            "ORDER_DELIVERING", orderId);
                }
                case DELIVERED, COMPLETED -> {
                    notificationService.sendNotification(order.getUser(),
                            "🎉 Giao hoa thành công đơn #" + orderId,
                            "Đơn hoa đã được giao đến nơi! Cảm ơn bạn đã lựa chọn tiệm hoa. Hãy để lại đánh giá cảm nhận nhé.",
                            "ORDER_DELIVERED", orderId);
                    if (order.getPickupStore() != null) {
                        notificationService.sendNotificationToStoreManagers(order.getPickupStore().getId(),
                                "✨ Đơn hoa #" + orderId + " đã hoàn tất",
                                "Đơn hoa #" + orderId + " đã giao thành công tới người nhận.",
                                "ORDER_COMPLETED", orderId);
                    }
                }
                case CANCELLED -> {
                    notificationService.sendNotification(order.getUser(),
                            "❌ Đơn hoa #" + orderId + " đã bị hủy",
                            order.getCancellationReason() != null ? order.getCancellationReason()
                                    : "Đơn hàng #" + orderId + " đã được hủy thành công.",
                            "ORDER_CANCELLED", orderId);
                    if (order.getPickupStore() != null) {
                        notificationService.sendNotificationToStoreManagers(order.getPickupStore().getId(),
                                "⚠️ Đơn hoa #" + orderId + " đã bị hủy",
                                order.getCancellationReason() != null ? order.getCancellationReason()
                                        : "Đơn hoa #" + orderId + " đã bị hủy bởi người dùng hoặc hệ thống.",
                                "ORDER_CANCELLED", orderId);
                    }
                }
                case REFUND_REQUESTED -> {
                    notificationService.sendNotification(order.getUser(), "Đơn #" + orderId + " đang chờ hoàn tiền",
                            order.getCancellationReason() != null ? order.getCancellationReason() : "Yêu cầu hoàn tiền đang chờ chi nhánh xử lý.",
                            "REFUND_REQUESTED", orderId);
                    if (order.getPickupStore() != null) {
                        notificationService.sendNotificationToStoreManagers(order.getPickupStore().getId(),
                                "📩 Yêu cầu hoàn tiền đơn #" + orderId,
                                order.getCancellationReason() != null ? order.getCancellationReason()
                                        : "Khách hàng " + order.getUser().getFullName() + " vừa gửi yêu cầu Trả hàng / Hoàn tiền cho đơn #" + orderId + ".",
                                "REFUND_REQUESTED", orderId);
                    }
                }
                case REFUNDED -> {
                    notificationService.sendNotification(order.getUser(),
                            "💰 Hoàn tiền thành công đơn #" + orderId,
                            "MOMO_DEMO".equals(order.getPaymentMethod())
                                    ? "Hoàn tiền thành công. Giao dịch demo, không phát sinh chuyển tiền MoMo thật."
                                    : "Yêu cầu hoàn tiền cho đơn hoa #" + orderId + " đã được cửa hàng xử lý hoàn tất.",
                            "REFUND_COMPLETED", orderId);
                }
                default -> {}
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(OrderWorkflowService.class).warn("Không gửi được notification trạng thái: {}", ex.getMessage());
        }
    }
}
