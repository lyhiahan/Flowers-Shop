package com.finalproject.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@Getter
public class MomoConfig {

    @Value("${momo.partner-code:MOMOBKUN20180529}")
    private String partnerCode;

    @Value("${momo.access-key:klm05TvNBzhg7h7j}")
    private String accessKey;

    @Value("${momo.secret-key:at67qH6mk8w5Y1nAyMoYKMWACiEi2AcA}")
    private String secretKey;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String endpoint;

    @Value("${momo.redirect-url:http://localhost:8080/checkout/momo/callback}")
    private String redirectUrl;

    @Value("${momo.ipn-url:http://localhost:8080/checkout/momo/ipn}")
    private String ipnUrl;

    /**
     * Ký HMAC SHA-256 theo chuẩn MoMo Payment Gateway
     */
    public String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh chữ ký bảo mật MoMo HMAC-SHA256", e);
        }
    }
}
