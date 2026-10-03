package com.finalproject.service.impl;

import com.finalproject.config.MomoConfig;
import com.finalproject.entity.Order;
import com.finalproject.entity.Payment;
import com.finalproject.enums.PaymentMethod;
import com.finalproject.enums.PaymentStatus;
import com.finalproject.repository.OrderRepository;
import com.finalproject.repository.PaymentRepository;
import com.finalproject.service.MomoService;
import com.finalproject.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomoServiceImpl implements MomoService {

    private final MomoConfig momoConfig;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Override
    public String createPaymentUrl(Order order, HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        String localFallbackUrl = baseUrl + "/checkout/momo/" + order.getId();

        try {
            long amount = order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 50000L;
            if (amount < 1000) amount = 1000L;

            String partnerCode = momoConfig.getPartnerCode();
            String accessKey = momoConfig.getAccessKey();
            String requestId = UUID.randomUUID().toString();
            String momoOrderId = order.getId() + "_" + System.currentTimeMillis();
            String orderInfo = "Thanh toán đơn hoa #" + order.getId() + " - Tiệm Hoa Tươi Sài Gòn";
            String redirectUrl = momoConfig.getRedirectUrl();
            if (redirectUrl == null || redirectUrl.isBlank() || redirectUrl.contains("localhost:8080")) {
                redirectUrl = baseUrl + "/checkout/momo/callback";
            }
            String ipnUrl = baseUrl + "/checkout/momo/ipn";
            String extraData = String.valueOf(order.getId());
            String requestType = "captureWallet";

            // Sinh chữ ký bảo mật HMAC SHA-256
            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + ipnUrl +
                    "&orderId=" + momoOrderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + partnerCode +
                    "&redirectUrl=" + redirectUrl +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;

            String signature = momoConfig.hmacSha256(rawSignature);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("partnerName", "Hoa Tươi Sài Gòn");
            requestBody.put("storeId", "FlowerShop_" + (order.getPickupStore() != null ? order.getPickupStore().getId() : "1"));
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", momoOrderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", redirectUrl);
            requestBody.put("ipnUrl", ipnUrl);
            requestBody.put("lang", "vi");
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", requestType);
            requestBody.put("signature", signature);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Đang kết nối cổng MoMo Gateway cho đơn hàng #{}: {}", order.getId(), momoConfig.getEndpoint());
            ResponseEntity<Map> response = restTemplate.postForEntity(momoConfig.getEndpoint(), entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> responseMap = response.getBody();
                Object payUrl = responseMap.get("payUrl");
                Object resultCode = responseMap.get("resultCode");
                if (payUrl != null && (resultCode == null || "0".equals(String.valueOf(resultCode)))) {
                    log.info("Sinh payUrl MoMo thành công cho đơn #{}: {}", order.getId(), payUrl);
                    return payUrl.toString();
                }
                log.warn("MoMo Sandbox phản hồi mã lỗi: {}. Sử dụng giao diện thanh toán MoMo mô phỏng.", resultCode);
            }
        } catch (Exception ex) {
            log.warn("Không kết nối trực tiếp được MoMo Gateway ({}). Tự động kích hoạt trang quét mã MoMo QR nội bộ cho đơn #{}.", ex.getMessage(), order.getId());
        }

        // Tự động chuyển hướng đến giao diện thanh toán MoMo QR của hệ thống (fallback tin cậy)
        return localFallbackUrl;
    }

    @Override
    @Transactional
    public boolean processPaymentResult(Long orderId, int resultCode, String transId) {
        if (orderId == null) return false;
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;

        if (resultCode == 0) {
            // Thanh toán MoMo thành công
            order.setPaymentStatus("PAID");
            order.setPaymentMethod("MOMO");
            orderRepository.save(order);

            // Cập nhật bản ghi thanh toán
            var payments = paymentRepository.findByOrderId(order.getId());
            Payment payment;
            if (!payments.isEmpty()) {
                payment = payments.get(0);
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setMethod(PaymentMethod.MOMO);
                payment.setPaidAt(LocalDateTime.now());
            } else {
                payment = Payment.builder()
                        .order(order)
                        .method(PaymentMethod.MOMO)
                        .amount(order.getTotalAmount())
                        .status(PaymentStatus.SUCCESS)
                        .paidAt(LocalDateTime.now())
                        .build();
            }
            paymentRepository.save(payment);

            // Gửi thông báo cho khách và cửa hàng
            notificationService.sendNotification(order.getUser(),
                    "💖 Thanh toán MoMo thành công!",
                    "Đơn hoa #" + order.getId() + " đã được thanh toán thành công qua Ví MoMo với số tiền " +
                            String.format("%,d", order.getTotalAmount().longValue()) + " đ.",
                    "PAYMENT_SUCCESS", order.getId());

            if (order.getPickupStore() != null) {
                notificationService.sendNotificationToStoreManagers(order.getPickupStore().getId(),
                        "💰 Đã nhận thanh toán MoMo đơn #" + order.getId(),
                        "Khách " + order.getRecipientName() + " đã hoàn tất thanh toán MoMo cho đơn #" + order.getId() + ".",
                        "PAYMENT_SUCCESS", order.getId());
            }

            log.info("Đã cập nhật trạng thái thanh toán MoMo PAID cho đơn #{}", order.getId());
            return true;
        } else {
            log.warn("Thanh toán MoMo đơn #{} thất bại hoặc bị hủy (resultCode: {})", order.getId(), resultCode);
            return false;
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        if (request == null) return "http://localhost:8080";
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        if ((scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443)) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + serverPort;
    }
}
