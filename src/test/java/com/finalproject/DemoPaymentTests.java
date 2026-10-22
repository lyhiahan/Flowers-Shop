package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.DemoPaymentService;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.demo-payment.base-url=http://192.168.1.50:8080")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DemoPaymentTests {
    @Autowired DemoPaymentService demo;
    @Autowired com.finalproject.service.OrderFundsService funds;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired EntityManager entityManager;
    @Autowired Clock clock;
    @Autowired MockMvc mvc;
    User customer;
    Store store;
    Order order;

    @BeforeEach void setup() {
        customer = users.save(User.builder().username("demo-customer").email("demo@test.local")
                .password("unused").fullName("Demo customer").role(Role.USER).isActive(true).build());
        store = stores.save(Store.builder().name("Demo store").address("Test address").status(true).isOpen(true).build());
        order = fixture("100000");
        entityManager.flush();
    }

    Order fixture(String amount) {
        Order created = orders.save(Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY)
                .status(OrderStatus.PENDING).paymentMethod("MOMO_DEMO").paymentStatus("UNPAID")
                // H2 persists six fractional digits; make the fixture deadline exactly representable.
                .paymentExpiresAt(LocalDateTime.now(clock).plusMinutes(10).truncatedTo(java.time.temporal.ChronoUnit.MICROS))
                .totalAmount(new BigDecimal(amount)).build());
        payments.save(Payment.builder().order(created).method(PaymentMethod.MOMO_DEMO)
                .amount(created.getTotalAmount()).status(PaymentStatus.PENDING).build());
        return created;
    }

    String token() { return demo.create(List.of(order.getId()), customer.getUsername()); }

    @Test void anonymousPhoneUsesItsOwnCsrfSessionAndDesktopSeesSuccess() throws Exception {
        String token = token();
        var phone = mvc.perform(get("/demo-payment/" + token)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận thanh toán")))
                .andReturn();
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        preview("phone.html", phone.getResponse().getContentAsByteArray());
        CsrfToken csrf = (CsrfToken) phone.getRequest().getAttribute(CsrfToken.class.getName());
        MockHttpSession phoneSession = (MockHttpSession) phone.getRequest().getSession(false);
        mvc.perform(post("/demo-payment/" + token + "/confirm").session(phoneSession)
                .param(csrf.getParameterName(), csrf.getToken())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/customer/demo-payment/" + token + "/status").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        mvc.perform(get("/demo-payment/" + token)).andExpect(redirectedUrl("/demo-payment/" + token + "/success"));
        var success = mvc.perform(get("/demo-payment/" + token + "/success")).andExpect(status().isOk()).andReturn();
        preview("success.html", success.getResponse().getContentAsByteArray());
    }

    @Test void duplicateConfirmDoesNotRewriteReceiptOrTimestamp() {
        String token = token();
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.SUCCESS);
        Payment payment = payments.findByOrderId(order.getId()).get(0);
        LocalDateTime paidAt = payment.getPaidAt();
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.SUCCESS);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        assertThat(payment.getTransactionCode()).isEqualTo("DEMO-MOMO-" + order.getId());
        assertThat(payment.getMomoEnvironment()).isEqualTo("demo");
        assertThat(payments.findByOrderId(order.getId())).hasSize(1);
    }

    @Test void wholeCheckoutIsValidatedBeforeAnyOrderIsPaid() {
        Order second = fixture("200000");
        entityManager.flush();
        String token = demo.create(List.of(second.getId(), order.getId()), customer.getUsername());
        assertThat(demo.view(token, customer.getUsername()).total()).isEqualByComparingTo("300000");
        second.setStatus(OrderStatus.CANCELLED);
        entityManager.flush();
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.FAILED);
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(payments.findByOrderId(order.getId()).get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test void successfulCheckoutPaysAllOrdersWithoutChangingFulfillment() {
        Order second = fixture("200000");
        entityManager.flush();
        String token = demo.create(List.of(order.getId(), second.getId()), customer.getUsername());
        demo.confirm(token);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(second.getPaymentStatus()).isEqualTo("PAID");
        assertThat(second.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test void expiredOrderCannotPayAndReopeningDoesNotExtendDeadline() {
        String token = token();
        Instant expiry = demo.view(token, customer.getUsername()).expiresAt();
        String reopened = token();
        assertThat(demo.view(reopened, customer.getUsername()).expiresAt()).isEqualTo(expiry);
        order.setPaymentExpiresAt(LocalDateTime.now(clock).minusSeconds(1));
        entityManager.flush();
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.EXPIRED);
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThatThrownBy(this::token).isInstanceOf(IllegalStateException.class);
    }

    @Test void tamperingWithStoredAmountCannotPay() {
        String token = token();
        order.setTotalAmount(BigDecimal.ONE);
        entityManager.flush();
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.FAILED);
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void disabledCustomerCannotConfirmThroughAnonymousPhone() throws Exception {
        String token = token();
        customer.setIsActive(false);
        entityManager.flush();
        mvc.perform(post("/demo-payment/" + token + "/confirm").with(csrf())).andExpect(status().isForbidden());
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void missingTokenCsrfAndOwnershipAreEnforced() throws Exception {
        String token = token();
        mvc.perform(get("/demo-payment/invalid")).andExpect(status().isNotFound());
        mvc.perform(get("/demo-payment/" + "0".repeat(64))).andExpect(status().isNotFound());
        mvc.perform(post("/demo-payment/" + token + "/confirm")).andExpect(status().isForbidden());
        users.saveAndFlush(User.builder().username("other-demo-customer").email("other-demo@test.local")
                .password("unused").fullName("Other").isActive(true).role(Role.USER).build());
        for (String suffix : List.of("", "/status", "/qr", "/success", "/connection"))
            mvc.perform(get("/customer/demo-payment/" + token + suffix).with(user("other-demo-customer").roles("USER")))
                    .andExpect(status().isForbidden());
        mvc.perform(post("/customer/demo-payment/order/" + order.getId()).with(user("other-demo-customer").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void qrDecodesToLanPhonePageAndDesktopActuallyRenders() throws Exception {
        String token = token();
        var desktop = mvc.perform(get("/customer/demo-payment/" + token).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("http://192.168.1.50:8080/demo-payment/" + token))).andReturn();
        preview("desktop.html", desktop.getResponse().getContentAsByteArray());
        byte[] png = mvc.perform(get("/customer/demo-payment/" + token + "/qr").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png")).andReturn().getResponse().getContentAsByteArray();
        var image = ImageIO.read(new ByteArrayInputStream(png));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        var bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));
        assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo("http://192.168.1.50:8080/demo-payment/" + token);
        preview("qr.png", png);
    }

    @Test void realMomoOrdersCannotEnterDemoFlow() {
        order.setPaymentMethod("MOMO");
        entityManager.flush();
        assertThatThrownBy(this::token).isInstanceOf(IllegalStateException.class);
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void successPagesRequirePaymentAndStayVisibleAfterConfirmation() throws Exception {
        String token = token();
        mvc.perform(get("/demo-payment/" + token + "/success"))
                .andExpect(redirectedUrl("/demo-payment/" + token));
        mvc.perform(get("/customer/demo-payment/" + token + "/success").with(user(customer.getUsername()).roles("USER")))
                .andExpect(redirectedUrl("/customer/demo-payment/" + token));
        var pending = mvc.perform(get("/demo-payment/" + token)).andExpect(status().isOk()).andReturn();
        assertThat(pending.getResponse().getContentAsString()).doesNotContain("Thanh toán thành công");
        mvc.perform(post("/demo-payment/" + token + "/confirm").with(csrf()))
                .andExpect(redirectedUrl("/demo-payment/" + token + "/success"));
        var success = mvc.perform(get("/customer/demo-payment/" + token + "/success").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("customer/demo-payment-success")).andReturn();
        assertThat(success.getResponse().getContentAsString()).contains("Thanh toán thành công", "Xem đơn hàng", "DEMO")
                .doesNotContain("payment-qr", "demo-payment.js");
        preview("desktop-success.html", success.getResponse().getContentAsByteArray());
    }

    @Test void productionCannotEnableLocalPaymentEvenWithFlagSet() {
        var environment = new org.springframework.mock.env.MockEnvironment();
        environment.setActiveProfiles("prod");
        var service = new DemoPaymentService(orders, payments, users, entityManager, clock, environment, funds);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "enabled", true);
        assertThat(service.isEnabled()).isFalse();
        assertThatThrownBy(() -> service.create(List.of(order.getId()), customer.getUsername()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    private void preview(String name, byte[] content) throws Exception {
        String directory = System.getProperty("demo.preview.dir");
        if (directory == null) return;
        var path = java.nio.file.Path.of(directory);
        java.nio.file.Files.createDirectories(path);
        java.nio.file.Files.write(path.resolve(name), content);
    }
}
