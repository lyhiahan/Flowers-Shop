package com.finalproject.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.*;

/** Sends to the requested recipient, from the site's verified sender; never needs a Gmail App Password. */
@Service
@Slf4j
public class BrevoOtpSender {
    private final RestClient client;

    @Value("${app.mail.brevo.api-key:}") private String apiKey = "";
    @Value("${app.mail.from-email:}") private String fromEmail = "";
    @Value("${app.mail.from-name:Hoa Tươi Sài Gòn}") private String fromName = "Hoa Tươi Sài Gòn";

    public BrevoOtpSender(@Lazy @Qualifier("otpMailRestClient") RestClient client) { this.client = client; }

    private String sanitizeFromName(String name) {
        if (name == null || name.isBlank()) return "Hoa Tươi Sài Gòn";
        if (name.contains("Æ") || name.contains("Ã") || name.contains("Â") || name.contains("â")) {
            try {
                String fixed = new String(name.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), java.nio.charset.StandardCharsets.UTF_8);
                if (!fixed.isBlank()) {
                    return fixed;
                }
            } catch (Exception ignored) {
            }
        }
        return name;
    }

    public void send(String recipient, String subject, String text) {
        if (apiKey == null || apiKey.trim().length() < 20 || apiKey.contains("placeholder") || apiKey.trim().startsWith("replace_")) {
            log.warn("BREVO_API_KEY thiếu hoặc chưa đầy đủ: email OTP chưa được gửi.");
            throw new IllegalStateException("Chưa cấu hình dịch vụ gửi email. Vui lòng liên hệ quản trị viên.");
        }
        if (fromEmail == null || fromEmail.isBlank() || fromEmail.trim().equalsIgnoreCase("your_verified_sender@example.com")) {
            log.warn("Thiếu MAIL_FROM_EMAIL: cần địa chỉ gửi đã xác minh tại nhà cung cấp mail.");
            throw new IllegalStateException("Chưa cấu hình địa chỉ gửi email. Vui lòng liên hệ quản trị viên.");
        }
        String sender;
        try { sender = OtpEmailInput.address(fromEmail); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("Địa chỉ gửi email chưa được cấu hình hợp lệ."); }
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", sender, "name", sanitizeFromName(fromName)),
                "to", List.of(Map.of("email", OtpEmailInput.address(recipient))),
                "subject", subject, "textContent", text);
        try {
            var response = client.post().uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey.trim()).body(body).retrieve().toEntity(Map.class);
            if (response.getStatusCode().value() != 201 || response.getBody() == null
                    || !(response.getBody().get("messageId") instanceof String id) || id.isBlank()) {
                log.warn("Mail API chưa xác nhận tiếp nhận email OTP (HTTP {}).", response.getStatusCode().value());
                throw new IllegalStateException("Không thể gửi email xác thực. Vui lòng thử lại sau.");
            }
            log.info("Mail API đã tiếp nhận email OTP để gửi.");
        } catch (RestClientResponseException ex) {
            // Do not log the response body: it may echo the API key, OTP, or email content.
            log.warn("Mail API từ chối gửi email OTP (HTTP {}).", ex.getStatusCode().value());
            throw new IllegalStateException("Không thể gửi email xác thực. Vui lòng thử lại sau.");
        } catch (RestClientException ex) {
            log.warn("Không kết nối được mail API. Loại lỗi: {}", ex.getClass().getSimpleName());
            throw new IllegalStateException("Không thể kết nối dịch vụ email. Vui lòng thử lại sau.");
        }
    }
}
