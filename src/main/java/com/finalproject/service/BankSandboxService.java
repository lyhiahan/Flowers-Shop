package com.finalproject.service;

import com.finalproject.config.BankTransferConfig;
import com.finalproject.entity.Payment;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;

/** Local test gateway; never contacts a bank or transfers money. */
@Service @RequiredArgsConstructor
public class BankSandboxService {
    private final BankTransferConfig config;
    private final UserRepository users;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final EntityManager entityManager;
    private final java.time.Clock clock;
    private final NotificationService notifications;

    @Transactional
    @PreAuthorize("hasRole('USER') and !hasAnyRole('ADMIN', 'MANAGER')")
    public String pay(Long id, String username, BigDecimal amount) {
        if (!config.isMock()) throw new IllegalStateException("Chỉ mô phỏng trong môi trường thử nghiệm.");
        var actor = users.findByUsername(username).orElseThrow();
        actor = users.findLockedById(actor.getId()).orElseThrow();
        entityManager.refresh(actor, LockModeType.PESSIMISTIC_WRITE);
        if (!Boolean.TRUE.equals(actor.getIsActive()) || Boolean.TRUE.equals(actor.getAccountDisabledByAdmin())
                || actor.isRetiredDeliveryAccount() || actor.getRole() != Role.USER
                || actor.hasRole(Role.ADMIN) || actor.hasRole(Role.MANAGER))
            throw new AccessDeniedException("Chỉ khách hàng đang hoạt động được thanh toán thử nghiệm.");
        var order = orders.findLockedById(id).orElseThrow();
        entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE);
        if (!order.getUser().getId().equals(actor.getId())) throw new AccessDeniedException("Không phải đơn của bạn.");
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod()) || amount == null || amount.signum() <= 0
                || amount.compareTo(order.getTotalAmount()) != 0
                || !EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING).contains(order.getStatus()))
            throw new IllegalStateException("Thông tin giao dịch hoặc trạng thái đơn không hợp lệ.");
        if ("UNPAID".equals(order.getPaymentStatus()) && (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED) && order.getPaymentExpiresAt() != null
                && !order.getPaymentExpiresAt().isAfter(LocalDateTime.now(clock)))
            throw new IllegalStateException("Đơn đã hết thời hạn thanh toán.");
        var records = payments.findByOrderId(id);
        if (records.size() != 1) throw new IllegalStateException("Không xác minh được bản ghi thanh toán.");
        Payment payment = records.get(0);
        if (payment.getMethod() != PaymentMethod.BANK_TRANSFER || payment.getAmount().compareTo(amount) != 0)
            throw new IllegalStateException("Số tiền giao dịch không khớp.");
        if ("PAID".equals(order.getPaymentStatus()) && payment.getStatus() == PaymentStatus.SUCCESS
                && ("DEV-BANK-" + id).equals(payment.getTransactionCode())) return payment.getTransactionCode();
        if (!"UNPAID".equals(order.getPaymentStatus()) || payment.getStatus() != PaymentStatus.PENDING)
            throw new IllegalStateException("Giao dịch không còn chờ thanh toán.");
        String receipt = "DEV-BANK-" + id;
        payment.setTransactionCode(receipt); payment.setStatus(PaymentStatus.SUCCESS); payment.setPaidAt(LocalDateTime.now(clock));
        order.setPaymentStatus("PAID"); payments.save(payment); orders.save(order);
        notifications.sendNotification(actor, "Thanh toán thử nghiệm thành công", "Đơn #" + id
                + " đã thanh toán thử nghiệm; không phát sinh chuyển tiền thật.", "PAYMENT_SUCCESS", id);
        return receipt;
    }
}
