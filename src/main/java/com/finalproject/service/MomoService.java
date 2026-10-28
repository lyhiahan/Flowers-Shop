package com.finalproject.service;

import com.finalproject.config.MomoConfig;
import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MomoService {
    private final MomoConfig config;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final MomoPaymentAttemptRepository attempts;
    private final NotificationService notifications;
    private final PlatformTransactionManager transactionManager;
    private final EntityManager em;
    private final java.time.Clock clock;
    private final OrderWorkflowService workflow;

    public String createPaymentUrl(Order input, HttpServletRequest request) {
        config.requireEnabled();
        // Commit the correlation record BEFORE contacting MoMo, so early IPN can find it.
        MomoPaymentAttempt attempt = new TransactionTemplate(transactionManager).execute(tx -> {
            Order order = orders.findLockedById(input.getId()).orElseThrow();
            requireActiveOwner(order);
            validatePayable(order);
            Payment payment = payment(order);
            if (payment.getStatus() != PaymentStatus.PENDING) throw new IllegalStateException("Giao dịch không còn chờ thanh toán.");
            payment.setMomoEnvironment(config.getMode());
            payments.save(payment);
            return attempts.saveAndFlush(MomoPaymentAttempt.builder().order(order).amount(order.getTotalAmount())
                    .environment(config.getMode()).requestId(UUID.randomUUID().toString())
                    .gatewayOrderId(order.getId() + "_" + UUID.randomUUID())
                    .orderInfo("Thanh toan don hoa #" + order.getId())
                    .extraData(Base64.getEncoder().encodeToString(("{\"orderId\":" + order.getId() + "}").getBytes(StandardCharsets.UTF_8)))
                    .build());
        });
        if (config.isMock()) return "/checkout/momo/" + input.getId();
        long amount = exactAmount(attempt.getAmount());
        if (amount < 1000 || amount > 50000000) throw new IllegalArgumentException("MoMo Test hỗ trợ số tiền từ 1.000 đến 50.000.000 đồng.");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", config.getPartnerCode()); body.put("requestId", attempt.getRequestId());
        body.put("amount", amount); body.put("orderId", attempt.getGatewayOrderId());
        body.put("orderInfo", attempt.getOrderInfo()); body.put("redirectUrl", config.getRedirectUrl());
        body.put("ipnUrl", config.getIpnUrl()); body.put("extraData", attempt.getExtraData());
        body.put("requestType", "captureWallet"); body.put("lang", "vi"); body.put("autoCapture", true);
        String raw = "accessKey=" + config.getAccessKey() + "&amount=" + amount + "&extraData=" + attempt.getExtraData()
                + "&ipnUrl=" + config.getIpnUrl() + "&orderId=" + attempt.getGatewayOrderId()
                + "&orderInfo=" + attempt.getOrderInfo() + "&partnerCode=" + config.getPartnerCode()
                + "&redirectUrl=" + config.getRedirectUrl() + "&requestId=" + attempt.getRequestId() + "&requestType=captureWallet";
        body.put("signature", config.hmacSha256(raw));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); factory.setReadTimeout(30000);
        try {
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
            Map<?, ?> response = new RestTemplate(factory).postForObject(config.getEndpoint(), new HttpEntity<>(body, headers), Map.class);
            if (response == null || !"0".equals(String.valueOf(response.get("resultCode"))))
                throw new IllegalStateException("MoMo Test chưa tạo được giao dịch. Đơn vẫn chưa thanh toán; vui lòng thử lại.");
            if (!attempt.getGatewayOrderId().equals(response.get("orderId")) || !attempt.getRequestId().equals(response.get("requestId"))
                    || !config.getPartnerCode().equals(response.get("partnerCode")) || Long.parseLong(response.get("amount").toString()) != amount)
                throw new IllegalStateException("Phản hồi tạo giao dịch MoMo Test không khớp.");
            String payUrl = Objects.toString(response.get("payUrl"), "");
            String responseRaw = "accessKey=" + config.getAccessKey() + "&amount=" + response.get("amount")
                    + "&message=" + response.get("message") + "&orderId=" + response.get("orderId")
                    + "&partnerCode=" + response.get("partnerCode") + "&payUrl=" + payUrl + "&requestId=" + response.get("requestId")
                    + "&responseTime=" + response.get("responseTime") + "&resultCode=" + response.get("resultCode");
            String signature = Objects.toString(response.get("signature"), "");
            if (!signature.matches("[0-9a-fA-F]{64}") || !java.security.MessageDigest.isEqual(
                    HexFormat.of().parseHex(config.hmacSha256(responseRaw)), HexFormat.of().parseHex(signature)))
                throw new IllegalStateException("Chữ ký tạo giao dịch MoMo Test không hợp lệ.");
            URI uri = URI.create(payUrl);
            if (!"https".equals(uri.getScheme()) || !"test-payment.momo.vn".equals(uri.getHost()))
                throw new IllegalStateException("Đường dẫn thanh toán không thuộc MoMo Test.");
            return payUrl;
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new IllegalStateException("Không kết nối được MoMo Test. Đơn vẫn chưa thanh toán; vui lòng thử lại.");
        }
    }

     @Transactional
    public boolean processNotification(Map<String, Object> payload) {
        if (config.isMock() || !config.verifyNotification(payload)) throw new SecurityException("Invalid MoMo signature or mode");
        if (!config.getPartnerCode().equals(MomoConfig.required(payload, "partnerCode"))) throw new IllegalArgumentException("Wrong partner");
        MomoPaymentAttempt attempt = attempts.findByGatewayOrderId(MomoConfig.required(payload, "orderId"))
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment attempt"));
        Order order = orders.findLockedById(attempt.getOrder().getId()).orElseThrow();
        em.refresh(attempt);
        long amount = Long.parseLong(MomoConfig.required(payload, "amount"));
        if (!"sandbox".equals(attempt.getEnvironment()) || !attempt.getRequestId().equals(MomoConfig.required(payload, "requestId"))
                || amount != exactAmount(attempt.getAmount()) || amount != exactAmount(order.getTotalAmount())
                || !attempt.getOrderInfo().equals(MomoConfig.required(payload, "orderInfo"))
                || !attempt.getExtraData().equals(MomoConfig.required(payload, "extraData"))
                || !"momo_wallet".equals(MomoConfig.required(payload, "orderType")))
            throw new IllegalArgumentException("Payment mismatch");
        int result = Integer.parseInt(MomoConfig.required(payload, "resultCode"));
        if (result != 0) return false;
        String transactionId = MomoConfig.required(payload, "transId");
        if (Long.parseLong(transactionId) <= 0) throw new IllegalArgumentException("Invalid transaction ID");
        if (attempt.isCompleted()) {
            if (!transactionId.equals(attempt.getTransactionId())) throw new IllegalArgumentException("Transaction mismatch");
            return true; // Replayed IPN must not re-send notifications or revive refunded orders.
        }
        workflow.expireUnpaid(order.getId(), LocalDateTime.now(clock));
        boolean expired = Boolean.TRUE.equals(order.getPaymentExpired()) && order.getStatus() == OrderStatus.CANCELLED;
        if (!expired) validatePayable(order);
        Payment payment = payment(order);
        if (!"sandbox".equals(payment.getMomoEnvironment())) throw new IllegalArgumentException("Wrong payment mode");
        if (expired) {
            if (payment.getStatus() != PaymentStatus.FAILED) throw new IllegalStateException("Thanh toán hết hạn không hợp lệ.");
            payment.setStatus(PaymentStatus.PENDING);
        }
        complete(order, payment, transactionId, "sandbox");
        if (expired) {
            order.setStatus(OrderStatus.REFUND_REQUESTED);
            order.setCancellationReason("MoMo xác nhận tiền sau khi đơn hết hạn. Cần hoàn tiền; không tiếp tục giao hàng.");
            orders.save(order);
            notifications.sendNotification(order.getUser(), "Đơn hết hạn cần hoàn tiền",
                    order.getCancellationReason(), "REFUND_REQUESTED", order.getId());
        }
        attempt.setCompleted(true); attempt.setTransactionId(transactionId); attempts.save(attempt);
        return true;
    }

     @Transactional
    public void simulatePayment(Long id, String username) {
        config.requireEnabled();
        if (!config.isMock()) throw new IllegalStateException("Mô phỏng chỉ dùng trong chế độ mock.");
        Order order = orders.findLockedById(id).orElseThrow();
        if (!order.getUser().getUsername().equals(username)) throw new org.springframework.security.access.AccessDeniedException("Không phải đơn của bạn.");
        requireActiveOwner(order);
        Payment payment = payment(order);
        if ("PAID".equals(order.getPaymentStatus()) && "mock".equals(payment.getMomoEnvironment())
                && payment.getStatus() == PaymentStatus.SUCCESS) return;
        validatePayable(order);
        if (payment.getMomoEnvironment() != null && !"mock".equals(payment.getMomoEnvironment()))
            throw new IllegalStateException("Không mô phỏng giao dịch sandbox.");
        complete(order, payment, "DEV-MOMO-" + id, "mock");
    }

    private void requireActiveOwner(Order order) {
        if (!Boolean.TRUE.equals(order.getUser().getIsActive()) || !order.getUser().isCustomerAccount()) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }
    }

    private void validatePayable(Order order) {
        if ((order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED)
                && order.getPaymentExpiresAt() != null && !order.getPaymentExpiresAt().isAfter(LocalDateTime.now(clock)))
            throw new IllegalStateException("Đơn đã hết thời hạn thanh toán.");
        if (!"MOMO".equals(order.getPaymentMethod()) || !"UNPAID".equals(order.getPaymentStatus())
                || EnumSet.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED, OrderStatus.REFUND_REQUESTED, OrderStatus.COMPLETED).contains(order.getStatus()))
            throw new IllegalStateException("Đơn không còn chờ thanh toán MoMo.");
        exactAmount(order.getTotalAmount());
    }

    private long exactAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Số tiền MoMo không hợp lệ.");
        try { return amount.longValueExact(); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("Số tiền MoMo phải là số đồng nguyên."); }
    }

    private Payment payment(Order order) {
        List<Payment> records = payments.findByOrderId(order.getId());
        if (records.size() != 1 || records.get(0).getMethod() != PaymentMethod.MOMO
                || records.get(0).getAmount().compareTo(order.getTotalAmount()) != 0)
            throw new IllegalStateException("Bản ghi thanh toán MoMo không hợp lệ.");
        return records.get(0);
    }

    private void complete(Order order, Payment payment, String transactionId, String environment) {
        if (payment.getStatus() != PaymentStatus.PENDING) throw new IllegalStateException("Giao dịch không còn chờ thanh toán.");
        order.setPaymentStatus("PAID"); orders.save(order);
        payment.setStatus(PaymentStatus.SUCCESS); payment.setPaidAt(LocalDateTime.now(clock));
        payment.setTransactionCode(transactionId); payment.setMomoEnvironment(environment); payments.save(payment);
        String label = "mock".equals(environment) ? "[DEV - mô phỏng]" : "[MoMo Test]";
        notifications.sendNotification(order.getUser(), label + " Thanh toán thành công",
                label + " Đơn hoa #" + order.getId() + " đã hoàn tất thanh toán thử nghiệm, không có tiền thật được chuyển.", "PAYMENT_SUCCESS", order.getId());
    }
}
