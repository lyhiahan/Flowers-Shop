package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Non-cash demo credits only. Every mutation belongs to the order's transaction. */
@Service @RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CustomerWalletService {
    private final CustomerWalletRepository wallets;
    private final CustomerWalletEntryRepository entries;
    private final UserRepository users;
    private final PaymentRepository payments;
    private final StoreAccessService access;
    private final DemoPaymentService demoPayments;
    private final OrderFundsService funds;
    private final EntityManager entityManager;
    private final Clock clock;

    public record Entry(Long id, Long orderId, String type, BigDecimal amount,
                        BigDecimal balanceAfter, LocalDateTime createdAt) {}

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public boolean isEnabled() { return demoPayments.isEnabled(); }

    @Transactional(readOnly = true)
    public BigDecimal balance(String username) {
        User owner = requireOwner(username);
        return wallets.findById(owner.getId()).map(CustomerWallet::getBalance).orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Page<Entry> history(String username, int page) {
        return history(username, page, null, null);
    }

    @Transactional(readOnly = true)
    public Page<Entry> history(String username, int page, LocalDate fromDate, LocalDate toDate) {
        User owner = requireOwner(username);
        if ((fromDate != null && (fromDate.getYear() < 1 || fromDate.getYear() > 9999))
                || (toDate != null && (toDate.getYear() < 1 || toDate.getYear() > 9999)))
            throw new IllegalArgumentException("Ngày lọc không hợp lệ. Vui lòng chọn lại ngày.");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw new IllegalArgumentException("Từ ngày không được sau đến ngày.");
        LocalDateTime fromTime = fromDate == null ? null : fromDate.atStartOfDay();
        // The SQL Server datetime2 range ends on 9999-12-31, so that end date is unbounded.
        LocalDateTime untilTime = toDate == null || toDate.equals(LocalDate.of(9999, 12, 31))
                ? null : toDate.plusDays(1).atStartOfDay();
        return entries.findHistory(owner.getId(), fromTime, untilTime, PageRequest.of(Math.max(0, page), 15))
                .map(e -> new Entry(e.getId(), e.getOrder().getId(), e.getType(), e.getAmount(),
                        e.getBalanceAfter(), e.getCreatedAt()));
    }

    public void pay(Order order, Payment receipt) {
        if (!isEnabled()) throw new IllegalStateException("Thanh toán bằng ví đang tắt.");
        User owner = requireOwner(order.getUser().getUsername());
        if (!owner.getId().equals(order.getUser().getId())) throw new AccessDeniedException("Ví không thuộc tài khoản của bạn.");
        if (!"WALLET_DEMO".equals(order.getPaymentMethod()) || receipt.getMethod() != PaymentMethod.WALLET_DEMO
                || receipt.getStatus() != PaymentStatus.PENDING || !"UNPAID".equals(order.getPaymentStatus())
                || order.getStatus() != OrderStatus.PENDING || !receipt.getOrder().getId().equals(order.getId()))
            throw new IllegalStateException("Đơn hàng không hợp lệ để thanh toán bằng ví.");
        BigDecimal amount = money(order.getTotalAmount());
        if (receipt.getAmount() == null || receipt.getAmount().compareTo(amount) != 0
                || entries.findByOrderIdAndType(order.getId(), "PAYMENT").isPresent())
            throw new IllegalStateException("Giao dịch ví không khớp hoặc đã được xử lý.");
        CustomerWallet wallet = lockedWallet(owner);
        if (wallet.getBalance().compareTo(amount) < 0) throw new IllegalStateException("Số dư ví không đủ để thanh toán.");
        wallet.setBalance(wallet.getBalance().subtract(amount));
        append(wallet, order, "PAYMENT", amount);
        receipt.setStatus(PaymentStatus.SUCCESS);
        receipt.setPaidAt(LocalDateTime.now(clock));
        receipt.setMomoEnvironment("demo-wallet");
        receipt.setTransactionCode("WALLET-DEMO-" + order.getId());
        order.setPaymentStatus("PAID");
        payments.save(receipt);
        funds.holdPaidOrders(List.of(order));
    }

    public void validateRefund(Order order) {
        if (!OrderFundsService.isDemoFunded(order) || !"PAID".equals(order.getPaymentStatus()))
            throw new IllegalStateException("Chỉ hoàn giao dịch demo đã thanh toán vào ví demo.");
        var records = payments.findByOrderId(order.getId());
        if (records.size() != 1) throw new IllegalStateException("Không tìm thấy thanh toán hợp lệ để hoàn vào ví.");
        Payment receipt = records.getFirst();
        BigDecimal amount = money(order.getTotalAmount());
        boolean fromWallet = "WALLET_DEMO".equals(order.getPaymentMethod());
        if (receipt.getStatus() != PaymentStatus.SUCCESS
                || receipt.getMethod() != (fromWallet ? PaymentMethod.WALLET_DEMO : PaymentMethod.MOMO_DEMO)
                || receipt.getAmount() == null || receipt.getAmount().compareTo(amount) != 0
                || !(fromWallet ? "demo-wallet" : "demo").equals(receipt.getMomoEnvironment())
                || !((fromWallet ? "WALLET-DEMO-" : "DEMO-MOMO-") + order.getId()).equals(receipt.getTransactionCode()))
            throw new IllegalStateException("Thanh toán không khớp đơn hàng để hoàn vào ví demo.");
        if (fromWallet) {
            var debit = entries.findByOrderIdAndType(order.getId(), "PAYMENT")
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy khoản đã trừ từ ví."));
            if (!debit.getWallet().getUserId().equals(order.getUser().getId()) || debit.getAmount().compareTo(amount) != 0)
                throw new IllegalStateException("Khoản đã trừ không khớp ví hoặc số tiền.");
        }
        if (entries.findByOrderIdAndType(order.getId(), "REFUND").isPresent())
            throw new IllegalStateException("Đơn hàng đã được hoàn vào ví.");
    }

    public void creditRefund(Order order) {
        validateRefund(order);
        CustomerWallet wallet = lockedWallet(order.getUser());
        BigDecimal amount = money(order.getTotalAmount());
        wallet.setBalance(money(wallet.getBalance().add(amount)));
        append(wallet, order, "REFUND", amount);
    }

    private User requireOwner(String username) {
        User owner = access.currentUser();
        if (!owner.hasRole(Role.USER) || owner.hasRole(Role.ADMIN) || !owner.getUsername().equals(username))
            throw new AccessDeniedException("Bạn chỉ được sử dụng ví của mình.");
        return owner;
    }

    private CustomerWallet lockedWallet(User owner) {
        // The user lock also serializes creation of the first wallet row.
        User locked = users.findLockedById(owner.getId()).orElseThrow();
        CustomerWallet wallet = wallets.findById(owner.getId()).orElse(null);
        if (wallet == null) return wallets.saveAndFlush(CustomerWallet.builder().userId(owner.getId()).user(locked).build());
        entityManager.refresh(wallet, LockModeType.PESSIMISTIC_WRITE);
        return wallet;
    }

    private void append(CustomerWallet wallet, Order order, String type, BigDecimal amount) {
        wallets.save(wallet);
        entries.saveAndFlush(CustomerWalletEntry.builder().wallet(wallet).order(order).type(type)
                .amount(amount).balanceAfter(wallet.getBalance()).createdAt(LocalDateTime.now(clock)).build());
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new IllegalStateException("Số tiền giao dịch ví không hợp lệ.");
        BigDecimal result = value.setScale(2, RoundingMode.UNNECESSARY);
        if (result.precision() > 18) throw new IllegalStateException("Số tiền vượt giới hạn ví.");
        return result;
    }
}
