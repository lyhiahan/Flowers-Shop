package com.finalproject.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@Getter
public class MomoConfig {
    @Value("${momo.mode:mock}") private String mode;
    @Value("${momo.partner-code:}") private String partnerCode;
    @Value("${momo.access-key:}") private String accessKey;
    @Value("${momo.secret-key:}") private String secretKey;
    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}") private String endpoint;
    @Value("${momo.redirect-url:http://localhost:8080/checkout/momo/callback}") private String redirectUrl;
    @Value("${momo.ipn-url:http://localhost:8080/checkout/momo/ipn}") private String ipnUrl;

    public boolean isMock() { return "mock".equals(mode); }

    @PostConstruct
    public void validate() {
        if (!isMock() && !"sandbox".equals(mode)) throw new IllegalStateException("MoMo chỉ hỗ trợ mock hoặc sandbox cho project dev.");
        if (!isMock()) {
            if (partnerCode.isBlank() || accessKey.isBlank() || secretKey.isBlank())
                throw new IllegalStateException("MoMo sandbox cần bộ khóa test của nhóm qua biến môi trường.");
            if (!"https://test-payment.momo.vn/v2/gateway/api/create".equals(endpoint))
                throw new IllegalStateException("Chế độ sandbox chỉ được gọi endpoint MoMo Test.");
        }
    }

    public String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("Không tạo được chữ ký MoMo.", ex); }
    }

    public boolean verifyNotification(Map<String, Object> payload) {
        if (isMock()) return false;
        try {
            String fields = "amount,extraData,message,orderId,orderInfo,orderType,partnerCode,payType,requestId,responseTime,resultCode,transId";
            String raw = "accessKey=" + accessKey + "&" + java.util.Arrays.stream(fields.split(","))
                    .map(key -> key + "=" + required(payload, key)).collect(Collectors.joining("&"));
            String signature = required(payload, "signature");
            if (!signature.matches("[0-9a-fA-F]{64}")) return false;
            return MessageDigest.isEqual(HexFormat.of().parseHex(hmacSha256(raw)), HexFormat.of().parseHex(signature));
        } catch (IllegalArgumentException ex) { return false; }
    }

    public static String required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || !(value instanceof String || value instanceof Number))
            throw new IllegalArgumentException("Thiếu hoặc sai trường MoMo: " + key);
        return value.toString();
    }
}
