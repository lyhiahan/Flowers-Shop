package com.finalproject.service;

import com.finalproject.entity.Order;
import com.finalproject.entity.Payment;
import com.finalproject.entity.User;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Local simulation only. The unguessable QR token authorizes the phone confirmation. */
@Service
@RequiredArgsConstructor
public class DemoPaymentService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final EntityManager entityManager;
    private final Clock clock;
    private final org.springframework.core.env.Environment environment;
    private final OrderFundsService funds;
    @Value("${app.demo-payment.enabled:false}") private boolean enabled;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private record Session(String token, String username, Long userId, List<Long> orderIds,
                           Map<Long, BigDecimal> amounts, BigDecimal total, Instant expiresAt) {}
    public enum Status { PENDING, SUCCESS, EXPIRED, FAILED }
    public record View(String token, List<Long> orderIds, BigDecimal total, Instant expiresAt, Status status) {}

    public boolean isEnabled() {
        return enabled && !environment.matchesProfiles("prod");
    }

    @Transactional
    public String create(List<Long> orderIds, String username) {
        requireEnabled();
        if (orderIds == null || orderIds.isEmpty() || orderIds.size() > 50
                || orderIds.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Danh sach don hang khong hop le.");
        User actor = users.findByUsername(username).orElseThrow(() -> new AccessDeniedException("Unknown customer"));
        actor = users.findLockedById(actor.getId()).orElseThrow();
        entityManager.refresh(actor, LockModeType.PESSIMISTIC_WRITE);
        requireActiveCustomer(actor);
        List<Long> ids = orderIds.stream().distinct().sorted().toList();
        Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
        Instant expires = clock.instant().plus(Duration.ofMinutes(10));
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : ids) {
            Order order = lockedOrder(id);
            if (!order.getUser().getId().equals(actor.getId())) throw new AccessDeniedException("Not your order");
            Payment payment = payment(order);
            if (!pending(order, payment)) throw new IllegalStateException("Don khong con cho thanh toan gia lap.");
            if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0
                    || payment.getAmount() == null || payment.getAmount().compareTo(order.getTotalAmount()) != 0)
                throw new IllegalStateException("So tien khong hop le.");
            Instant deadline = deadline(order);
            if (deadline.isBefore(expires)) expires = deadline;
            amounts.put(id, order.getTotalAmount());
            total = total.add(order.getTotalAmount());
        }
        if (!expires.isAfter(clock.instant())) throw new IllegalStateException("Don da het han thanh toan.");
        // Keep completed/expired results briefly for retries; never retain tokens indefinitely.
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().plus(Duration.ofHours(1)).isBefore(clock.instant()));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        sessions.put(token, new Session(token, username, actor.getId(), ids, Map.copyOf(amounts), total, expires));
        return token;
    }

    @Transactional(readOnly = true)
    public View view(String token, String owner) {
        Session session = session(token);
        if (owner != null && !session.username().equals(owner)) throw new AccessDeniedException("Not your payment session");
        List<Order> found = session.orderIds().stream().map(id -> orders.findById(id).orElseThrow(this::missing)).toList();
        List<Payment> records = found.stream().map(this::payment).toList();
        return view(session, found, records);
    }

    @Transactional
    public View confirm(String token) {
        Session session = session(token);
        User actor = users.findLockedById(session.userId()).orElseThrow(this::missing);
        entityManager.refresh(actor, LockModeType.PESSIMISTIC_WRITE);
        requireActiveCustomer(actor);
        List<Order> found = session.orderIds().stream().map(this::lockedOrder).toList();
        List<Payment> records = found.stream().map(this::payment).toList();
        View current = view(session, found, records);
        if (current.status() != Status.PENDING) return current;
        // Validate every order before writing any result; rollback covers the whole checkout.
        for (int i = 0; i < found.size(); i++) {
            Order order = found.get(i);
            Payment payment = records.get(i);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now(clock));
            payment.setTransactionCode("DEMO-MOMO-" + order.getId());
            payment.setMomoEnvironment("demo");
            order.setPaymentStatus("PAID");
            payments.save(payment);
            orders.save(order);
        }
        funds.holdPaidOrders(found);
        return new View(session.token(), session.orderIds(), session.total(), session.expiresAt(), Status.SUCCESS);
    }

    private View view(Session session, List<Order> found, List<Payment> records) {
        Status status = Status.PENDING;
        boolean allPaid = true;
        boolean anyPaid = false;
        for (int i = 0; i < found.size(); i++) {
            Order order = found.get(i);
            Payment payment = records.get(i);
            if (!order.getUser().getId().equals(session.userId()) || !activeCustomer(order.getUser())
                    || !"MOMO_DEMO".equals(order.getPaymentMethod()) || payment.getMethod() != PaymentMethod.MOMO_DEMO
                    || order.getTotalAmount() == null || payment.getAmount() == null
                    || session.amounts().get(order.getId()).compareTo(order.getTotalAmount()) != 0
                    || payment.getAmount().compareTo(order.getTotalAmount()) != 0)
                return new View(session.token(), session.orderIds(), session.total(), session.expiresAt(), Status.FAILED);
            boolean paid = "PAID".equals(order.getPaymentStatus()) && payment.getStatus() == PaymentStatus.SUCCESS
                    && ("DEMO-MOMO-" + order.getId()).equals(payment.getTransactionCode());
            allPaid &= paid;
            anyPaid |= paid;
            if (!paid && !pending(order, payment)) status = Status.FAILED;
            if (!paid && !deadline(order).isAfter(clock.instant()) && status != Status.FAILED) status = Status.EXPIRED;
        }
        if (allPaid) status = Status.SUCCESS;
        else if (anyPaid) status = Status.FAILED;
        else if (!session.expiresAt().isAfter(clock.instant()) && status == Status.PENDING) status = Status.EXPIRED;
        return new View(session.token(), session.orderIds(), session.total(), session.expiresAt(), status);
    }

    private boolean pending(Order order, Payment payment) {
        return "MOMO_DEMO".equals(order.getPaymentMethod()) && payment.getMethod() == PaymentMethod.MOMO_DEMO
                && "UNPAID".equals(order.getPaymentStatus()) && payment.getStatus() == PaymentStatus.PENDING
                && EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING).contains(order.getStatus());
    }

    private Instant deadline(Order order) {
        return order.getPaymentExpiresAt() == null ? Instant.MIN : order.getPaymentExpiresAt().atZone(clock.getZone()).toInstant();
    }

    private Order lockedOrder(Long id) {
        Order order = orders.findLockedById(id).orElseThrow(this::missing);
        entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE);
        return order;
    }

    private Payment payment(Order order) {
        List<Payment> records = payments.findByOrderId(order.getId());
        if (records.size() != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid payment record");
        return records.get(0);
    }

    private Session session(String token) {
        requireEnabled();
        if (token == null || !token.matches("[0-9a-f]{64}")) throw missing();
        Session session = sessions.get(token);
        if (session == null) throw missing();
        return session;
    }

    private boolean activeCustomer(User user) {
        return Boolean.TRUE.equals(user.getIsActive()) && !Boolean.TRUE.equals(user.getAccountDisabledByAdmin())
                && !user.isRetiredDeliveryAccount() && user.hasRole(Role.USER)
                && !user.hasRole(Role.ADMIN) && !user.hasRole(Role.MANAGER);
    }

    private void requireActiveCustomer(User user) {
        if (!activeCustomer(user)) throw new AccessDeniedException("Customer is not active");
    }

    private void requireEnabled() {
        if (!isEnabled()) throw missing();
    }

    private ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment session not found");
    }
}
