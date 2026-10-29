package com.finalproject;

import com.finalproject.config.MomoConfig;
import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.MomoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"momo.mode=sandbox", "momo.partner-code=TEST_PARTNER", "momo.access-key=TEST_ACCESS", "momo.secret-key=TEST_SECRET"})
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class MomoSandboxSecurityTests {
    @Autowired MomoConfig config;
    @Autowired MomoService momo;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired MomoPaymentAttemptRepository attempts;
    @Autowired NotificationRepository notifications;
    Order order;
    Payment payment;
    MomoPaymentAttempt attempt;

    @BeforeEach void setup() {
        User user = users.save(User.builder().username("momo-customer").email("momo@test.local")
                .fullName("MoMo Test").password("unused-test-password").isActive(true).role(Role.USER).build());
        Store store = stores.save(Store.builder().name("Sandbox store").address("Test address").build());
        order = orders.saveAndFlush(Order.builder().user(user).pickupStore(store).orderType(OrderType.BUY)
                .status(OrderStatus.PENDING).paymentMethod("MOMO").paymentStatus("UNPAID").totalAmount(new BigDecimal("100000")).build());
        payment = payments.saveAndFlush(Payment.builder().order(order).method(PaymentMethod.MOMO)
                .amount(order.getTotalAmount()).status(PaymentStatus.PENDING).momoEnvironment("sandbox").build());
        attempt = attempts.saveAndFlush(MomoPaymentAttempt.builder().order(order).gatewayOrderId(order.getId()+"_TEST")
                .requestId("TEST-REQUEST").environment("sandbox").amount(order.getTotalAmount())
                .orderInfo("Test flowers").extraData("e30=").build());
    }

    Map<String,Object> payload() {
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("amount", 100000); p.put("extraData", "e30="); p.put("message", "Successful.");
        p.put("orderId", attempt.getGatewayOrderId()); p.put("orderInfo", "Test flowers");
        p.put("orderType", "momo_wallet"); p.put("partnerCode", "TEST_PARTNER"); p.put("payType", "qr");
        p.put("requestId", "TEST-REQUEST"); p.put("responseTime", 1712345678900L); p.put("resultCode", 0); p.put("transId", 123456);
        sign(p); return p;
    }

    void sign(Map<String,Object> p) {
        String raw = "accessKey=TEST_ACCESS&amount="+p.get("amount")+"&extraData="+p.get("extraData")+"&message="+p.get("message")
                +"&orderId="+p.get("orderId")+"&orderInfo="+p.get("orderInfo")+"&orderType="+p.get("orderType")
                +"&partnerCode="+p.get("partnerCode")+"&payType="+p.get("payType")+"&requestId="+p.get("requestId")
                +"&responseTime="+p.get("responseTime")+"&resultCode="+p.get("resultCode")+"&transId="+p.get("transId");
        p.put("signature", config.hmacSha256(raw));
    }

    String json(Map<String,Object> p) {
        return "{"+p.entrySet().stream().map(e -> "\""+e.getKey()+"\":\""+e.getValue()+"\"")
                .collect(java.util.stream.Collectors.joining(","))+"}";
    }

    @Test void validIpnIsIdempotentAndDoesNotReviveRefundedOrder() throws Exception {
        Map<String,Object> p=payload();
        assertThat(config.verifyNotification(p)).isTrue();
        mvc.perform(post("/checkout/momo/ipn").contentType("application/json").content(json(p))).andExpect(status().isNoContent());
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(payment.getTransactionCode()).isEqualTo("123456");
        long count=notifications.count();
        assertThat(momo.processNotification(p)).isTrue();
        assertThat(notifications.count()).isEqualTo(count);
        order.setStatus(OrderStatus.REFUNDED); order.setPaymentStatus("REFUNDED"); payment.setStatus(PaymentStatus.REFUNDED);
        orders.saveAndFlush(order); payments.saveAndFlush(payment);
        assertThat(momo.processNotification(p)).isTrue();
        assertThat(order.getPaymentStatus()).isEqualTo("REFUNDED");
    }

    @Test void tamperedOrMissingSignatureCannotPay() throws Exception {
        Map<String,Object> p=payload(); p.put("amount",100001);
        mvc.perform(post("/checkout/momo/ipn").contentType("application/json").content(json(p))).andExpect(status().isForbidden());
        p=payload(); p.remove("signature");
        mvc.perform(post("/checkout/momo/ipn").contentType("application/json").content(json(p))).andExpect(status().isForbidden());
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @ParameterizedTest @ValueSource(strings={"amount","requestId","partnerCode","orderId","orderInfo","extraData","orderType"})
    void evenSignedResponseMustMatchStoredAttempt(String field) throws Exception {
        Map<String,Object> p=payload(); p.put(field, field.equals("amount") ? "99999" : "WRONG"); sign(p);
        mvc.perform(post("/checkout/momo/ipn").contentType("application/json").content(json(p))).andExpect(status().isBadRequest());
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test void failedOrAuthorizedResultDoesNotBecomePaid() {
        Map<String,Object> p=payload(); p.put("resultCode",9000); sign(p);
        assertThat(momo.processNotification(p)).isFalse();
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void cancelledOrdersCannotBePaidByIpn() throws Exception {
        order.setStatus(OrderStatus.CANCELLED); orders.saveAndFlush(order);
        mvc.perform(post("/checkout/momo/ipn").contentType("application/json").content(json(payload()))).andExpect(status().isBadRequest());
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void sandboxCannotUseMockButtonAndAnotherCustomerCannotViewOrder() throws Exception {
        // Use a real active account so this checks authorization, not session revocation.
        users.saveAndFlush(User.builder().username("other-customer").email("other-customer@fixture.test")
                .password("unused-fixture-password").fullName("Access test").role(Role.USER).isActive(true).build());
        assertThatThrownBy(() -> momo.simulatePayment(order.getId(), "momo-customer")).hasMessageContaining("chế độ mock");
        mvc.perform(get("/checkout/momo/"+order.getId()).with(user("momo-customer").roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/simulate"))));
        mvc.perform(get("/checkout/momo/"+order.getId()).with(user("other-customer").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test void verifiedLatePaymentRequiresRefundAndReplayDoesNotReviveOrder() {
        order.setPaymentExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));orders.saveAndFlush(order);
        var notification=payload();
        assertThat(momo.processNotification(notification)).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getPaymentExpired()).isTrue();
        long count=notifications.count();
        assertThat(momo.processNotification(notification)).isTrue();
        assertThat(notifications.count()).isEqualTo(count);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
    }
}
