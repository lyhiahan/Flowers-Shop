package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.BankSandboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="bank-transfer.mode=mock")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class BankSandboxTests {
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired BankSandboxService sandbox;
    @Autowired MockMvc mvc;
    User customer; Order order; Payment payment;
    @BeforeEach void setup() {
        customer=users.save(User.builder().username("sandbox_customer").email("sandbox@test.local")
                .password("unused").fullName("Test customer").role(Role.USER).isActive(true).build());
        var store=stores.save(Store.builder().name("Bank fixture").address("123 Test street").status(true).isOpen(true).build());
        order=orders.save(Order.builder().user(customer).pickupStore(store).status(OrderStatus.PENDING)
                .orderType(OrderType.BUY)
                .paymentMethod("BANK_TRANSFER").paymentStatus("UNPAID").shippingMethod("PICKUP")
                .totalAmount(new BigDecimal("100000")).build());
        payment=payments.save(Payment.builder().order(order).method(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.PENDING).amount(order.getTotalAmount()).build());
        login("USER");
    }
    void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(customer.getUsername(), "",
                List.of(new SimpleGrantedAuthority("ROLE_"+role))));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void verifiedPaymentChangesOnlyPaymentAndRetryIsIdempotent() {
        assertThat(sandbox.pay(order.getId(),customer.getUsername(),order.getTotalAmount())).isEqualTo("DEV-BANK-"+order.getId());
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        sandbox.pay(order.getId(),customer.getUsername(),order.getTotalAmount());
        assertThat(payments.findByOrderId(order.getId())).hasSize(1);
    }
    @Test void incorrectAmountCannotMarkPaid() {
        assertThatThrownBy(()->sandbox.pay(order.getId(),customer.getUsername(),BigDecimal.ONE)).isInstanceOf(IllegalStateException.class);
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }
    @Test void cancelledOrderCannotBePaid() {
        order.setStatus(OrderStatus.CANCELLED); orders.saveAndFlush(order);
        assertThatThrownBy(()->sandbox.pay(order.getId(),customer.getUsername(),order.getTotalAmount())).isInstanceOf(IllegalStateException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
    @Test void mismatchedPaymentRecordCannotBePaid() {
        payment.setAmount(BigDecimal.ONE); payments.saveAndFlush(payment);
        assertThatThrownBy(()->sandbox.pay(order.getId(),customer.getUsername(),order.getTotalAmount())).isInstanceOf(IllegalStateException.class);
    }
    @Test void disabledCustomerCannotPayEvenWithOldSession() {
        customer.setIsActive(false); users.saveAndFlush(customer);
        assertThatThrownBy(()->sandbox.pay(order.getId(),customer.getUsername(),order.getTotalAmount()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void managerWithCustomerAuthorityCannotPay() {
        customer.setRoles(new java.util.HashSet<>(List.of(Role.MANAGER,Role.USER))); users.saveAndFlush(customer);
        assertThatThrownBy(()->sandbox.pay(order.getId(),customer.getUsername(),order.getTotalAmount()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void pageDoesNotExposeRealBankDetailsOrMutateOrder() throws Exception {
        var result=mvc.perform(get("/checkout/bank/"+order.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Giao dịch thử nghiệm", "bankSandboxForm")
                .doesNotContain("bank-transfer-techcombank.jpg", "3337110941", "BUI THANH PHUC");
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }
    @Test void browserSimulationRequiresServerSessionTokenAndCsrf() throws Exception {
        var session=new MockHttpSession();
        var page=mvc.perform(get("/checkout/bank/"+order.getId()).session(session).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn();
        String token=(String)page.getModelAndView().getModel().get("bankToken");
        mvc.perform(post("/checkout/bank/"+order.getId()+"/simulate").session(session).with(user(customer.getUsername()).roles("USER"))
                .param("token",token).param("amount","100000")).andExpect(status().isForbidden());
        mvc.perform(post("/checkout/bank/"+order.getId()+"/simulate").session(session).with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("token",token).param("amount","1")).andExpect(status().isBadRequest());
        mvc.perform(post("/checkout/bank/"+order.getId()+"/simulate").session(session).with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("token",token).param("amount","100000")).andExpect(status().is3xxRedirection());
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
    @Test void otherCustomersCannotReadOrPayOrder() throws Exception {
        // Use a real active account so this checks authorization, not session revocation.
        users.saveAndFlush(User.builder().username("someone_else").email("someone_else@fixture.test")
                .password("unused-fixture-password").fullName("Access test").role(Role.USER).isActive(true).build());
        mvc.perform(get("/checkout/bank/"+order.getId()).with(user("someone_else").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(post("/checkout/bank/"+order.getId()+"/simulate").with(user("someone_else").roles("USER")).with(csrf())
                .param("token","fake").param("amount","100000")).andExpect(status().isForbidden());
    }
}
