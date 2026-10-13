package com.finalproject.service;

import com.finalproject.entity.Order;
import com.finalproject.entity.Store;
import com.finalproject.entity.Transaction;
import com.finalproject.enums.OrderFundsStatus;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Internal demo ledger. Callers hold the order lock in the same transaction. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OrderFundsService {
    private final StoreRepository stores;
    private final TransactionRepository transactions;
    private final EntityManager entityManager;

    public void holdPaidOrders(List<Order> orders) {
        Map<Long, Store> lockedStores = new HashMap<>();
        // A checkout can span branches; all confirmations acquire branch locks in the same order.
        for (Long id : orders.stream().map(o -> o.getPickupStore().getId()).distinct().sorted().toList()) {
            Store store = stores.findLockedById(id).orElseThrow();
            entityManager.refresh(store, LockModeType.PESSIMISTIC_WRITE);
            lockedStores.put(id, store);
        }
        for (Order order : orders) {
            if (!"MOMO_DEMO".equals(order.getPaymentMethod()) || !"PAID".equals(order.getPaymentStatus()))
                throw new IllegalStateException("Chỉ giữ tiền cho giao dịch MoMo demo đã thanh toán.");
            if (order.getFundsStatus() == OrderFundsStatus.HELD) {
                validateSnapshot(order);
                continue;
            }
            if (order.getFundsStatus() != OrderFundsStatus.NONE)
                throw new IllegalStateException("Khoản tiền của đơn đã được xử lý hoặc cần đối soát.");
            Store store = lockedStores.get(order.getPickupStore().getId());
            BigDecimal rate = store.getCommission() == null ? BigDecimal.TEN : store.getCommission().getCost();
            validateRate(rate);
            BigDecimal amount = order.getTotalAmount();
            if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 16)
                throw new IllegalStateException("Số tiền giữ không hợp lệ.");
            order.setFundsAmount(amount);
            order.setCommissionRateSnapshot(rate.setScale(4, RoundingMode.UNNECESSARY));
            order.setFundsStatus(OrderFundsStatus.HELD);
            store.setPendingBalance(store.getPendingBalance().add(amount));
            stores.save(store);
            entry(order, "PAYMENT_HOLD", "PENDING", amount, true, "Nhận tiền chờ giao hàng đơn #" + order.getId());
        }
    }

    public BigDecimal settleHeld(Order order, Store store) {
        requireHeld(order);
        BigDecimal amount = order.getFundsAmount();
        BigDecimal income = amount.multiply(BigDecimal.ONE.subtract(order.getCommissionRateSnapshot().movePointLeft(2)))
                .setScale(2, RoundingMode.HALF_UP);
        release(order, store);
        entry(order, "HOLD_RELEASE", "PENDING", amount, false, "Chuyển tiền chờ sang thu nhập đơn #" + order.getId());
        BigDecimal fee = amount.subtract(income);
        if (fee.signum() > 0)
            entry(order, "COMMISSION_FEE", "PLATFORM", fee, true, "Hoa hồng đơn #" + order.getId());
        order.setFundsStatus(OrderFundsStatus.SETTLED);
        return income;
    }

    public boolean refundHeld(Order order, Store store) {
        if (!"MOMO_DEMO".equals(order.getPaymentMethod())) return false;
        if (order.getFundsStatus() == OrderFundsStatus.SETTLED) {
            validateSnapshot(order);
            return false;
        }
        requireHeld(order);
        if (!transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT").isEmpty())
            throw new IllegalStateException("Tiền chờ và thu nhập của đơn không khớp, cần đối soát.");
        release(order, store);
        entry(order, "REFUND", "PENDING", order.getFundsAmount(), false, "Hoàn tiền chờ cho khách đơn #" + order.getId());
        order.setFundsStatus(OrderFundsStatus.REFUNDED);
        return true;
    }

    public void reverseCommission(Order order) {
        for (Transaction fee : transactions.findAllByOrderIdAndType(order.getId(), "COMMISSION_FEE")) {
            entry(order, "COMMISSION_REFUND", "PLATFORM", fee.getAmount(), false, "Hoàn hoa hồng đơn #" + order.getId());
        }
    }

    private void requireHeld(Order order) {
        if (order.getFundsStatus() != OrderFundsStatus.HELD)
            throw new IllegalStateException("Đơn chưa có khoản tiền chờ hợp lệ, cần đối soát trước khi xử lý.");
        validateSnapshot(order);
    }

    private void validateSnapshot(Order order) {
        if (order.getFundsAmount() == null || order.getFundsAmount().signum() <= 0
                || order.getTotalAmount() == null || order.getFundsAmount().compareTo(order.getTotalAmount()) != 0)
            throw new IllegalStateException("Số tiền của đơn không khớp khoản đã giữ.");
        validateRate(order.getCommissionRateSnapshot());
    }

    private void validateRate(BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalStateException("Tỷ lệ hoa hồng phải nằm trong khoảng 0 đến 100%.");
    }

    private void release(Order order, Store store) {
        if (store.getPendingBalance() == null || store.getPendingBalance().compareTo(order.getFundsAmount()) < 0)
            throw new IllegalStateException("Số dư chờ không đủ, cần đối soát đơn hàng.");
        store.setPendingBalance(store.getPendingBalance().subtract(order.getFundsAmount()));
        stores.save(store);
    }

    private void entry(Order order, String type, String bucket, BigDecimal amount, boolean increase, String description) {
        transactions.save(Transaction.builder().order(order).store(order.getPickupStore()).amount(amount)
                .isUp(increase).walletBucket(bucket).type(type).status("COMPLETED")
                .requestKey("demo-funds:" + type + ":" + order.getId()).description(description).build());
    }
}
