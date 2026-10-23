package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.transaction.BeforeTransaction;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:acceptance-funds-${random.uuid};MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "app.demo-payment.auto-tunnel.enabled=false"
})
class CancellationWalletAcceptanceTests extends DemoPendingFundsTests {
    static final String PASSWORD = "Acceptance-only-password-2026";
    @Autowired PasswordEncoder encoder;
    @Autowired DataSource dataSource;
    User admin;

    @BeforeTransaction void requireDisposableDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:acceptance-funds-");
        }
    }

    @BeforeEach void prepareAcceptanceActors() {
        manager.setPassword(encoder.encode(PASSWORD));
        users.saveAndFlush(manager);
        admin = users.saveAndFlush(User.builder().username("acceptance-admin").email("acceptance-admin@test.local")
                .fullName("Acceptance admin").password(encoder.encode(PASSWORD)).role(Role.ADMIN).isActive(true).build());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = "   ")
    void managerCannotCancelWithoutAReason(String reason) throws Exception {
        Order order = fixture(); pay(order);
        var request = post("/manager/orders/update-status/" + order.getId())
                .with(user(manager.getUsername()).roles("MANAGER")).with(csrf()).param("status", "CANCELLED");
        if (reason != null) request.param("cancellationReason", reason);
        mvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertBalances("50000", "100000");
        assertThat(stock()).isEqualTo(9);
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "REFUND")).isEmpty();
    }

    @Test void managerUnpaidCancellationShowsActorAndReasonToCustomerAndRestocksOnce() throws Exception {
        Order order = fixture();
        for (int attempt = 0; attempt < 2; attempt++) managerTransition(order, "CANCELLED", "Khong du hoa de giao");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stock()).isEqualTo(10);
        assertBalances("50000", "0");
        mvc.perform(get("/customer/orders/" + order.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã hủy bởi Cửa hàng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khong du hoa de giao")));
    }

    @Test void managerPaidCancellationWaitsForExplicitRefundAndNeverRefundsOrRestocksTwice() throws Exception {
        Order first = fixture(), second = fixture(); pay(first); pay(second);
        managerTransition(first, "CANCELLED", "Khong du hoa de giao");
        assertThat(first.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        assertThat(first.getFundsStatus()).isEqualTo(OrderFundsStatus.HELD);
        assertBalances("50000", "200000");
        assertThat(stock()).isEqualTo(9);
        assertThat(transactions.findAllByOrderIdAndType(first.getId(), "REFUND")).isEmpty();
        mvc.perform(get("/customer/orders/" + first.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chờ hoàn tiền")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khong du hoa de giao")));
        mvc.perform(post("/manager/orders/update-status/" + first.getId()).with(user(manager.getUsername()).roles("MANAGER"))
                        .with(csrf()).param("status", "CANCELLED").param("cancellationReason", "Repeated cancellation"))
                .andExpect(flash().attributeExists("errorMessage"));
        managerTransition(first, "REFUNDED", null);
        managerTransition(first, "REFUNDED", null);
        assertBalances("50000", "100000");
        assertThat(stock()).isEqualTo(9);
        assertThat(first.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(first.getPaymentStatus()).isEqualTo("REFUNDED");
        assertThat(second.getFundsStatus()).isEqualTo(OrderFundsStatus.HELD);
        assertThat(transactions.findAllByOrderIdAndType(first.getId(), "REFUND")).singleElement()
                .satisfies(tx -> assertThat(tx.getAmount()).isEqualByComparingTo("100000"));
        mvc.perform(get("/customer/orders/" + first.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Đã hoàn tiền")));
    }

    @Test void deliveryReleasesPendingFundsBeforeTheFinalCompletedTransitionAndOnlyOnce() {
        Order order = fixture(); order.setShippingMethod("DELIVERY"); pay(order); login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        workflow.transition(order.getId(), OrderStatus.DELIVERING);
        assertBalances("50000", "100000");
        workflow.transition(order.getId(), OrderStatus.DELIVERED);
        assertBalances("140000", "0");
        workflow.transition(order.getId(), OrderStatus.DELIVERED);
        workflow.transition(order.getId(), OrderStatus.COMPLETED);
        assertBalances("140000", "0");
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT")).hasSize(1);
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "COMMISSION_FEE")).hasSize(1);
    }

    @ParameterizedTest @ValueSource(strings = {"0", "49999", "50001"})
    void invalidWithdrawalsCannotSpendThePendingBalance(String amount) throws Exception {
        Order order = fixture(); pay(order);
        withdrawal(amount, PASSWORD, UUID.randomUUID().toString()).andExpect(flash().attributeExists("errorMessage"));
        assertBalances("50000", "100000");
        assertThat(withdrawals()).isEmpty();
    }

    @Test void wrongWithdrawalPasswordNeverChangesTheBalance() throws Exception {
        withdrawal("50000", "wrong-password", UUID.randomUUID().toString()).andExpect(flash().attributeExists("errorMessage"));
        assertBalances("50000", "0");
        assertThat(withdrawals()).isEmpty();
    }

    @Test void validWithdrawalAndAdminRejectionReturnMoneyExactlyOnce() throws Exception {
        String requestId = UUID.randomUUID().toString();
        withdrawal("50000", PASSWORD, requestId).andExpect(flash().attributeExists("successMessage"));
        withdrawal("50000", PASSWORD, requestId).andExpect(flash().attributeExists("successMessage"));
        assertBalances("0", "0");
        assertThat(withdrawals()).hasSize(1);
        Transaction tx = withdrawals().getFirst();
        mvc.perform(post("/admin/transactions/" + tx.getId() + "/resolve").with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()).param("paid", "false")).andExpect(flash().attributeExists("successMessage"));
        assertBalances("50000", "0");
        assertThat(tx.getStatus()).isEqualTo("REJECTED");
        mvc.perform(post("/admin/transactions/" + tx.getId() + "/resolve").with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()).param("paid", "false")).andExpect(flash().attributeExists("errorMessage"));
        assertBalances("50000", "0");
        assertThat(withdrawals()).hasSize(1);
    }

    @Test void missingAdminDecisionIsRejectedWithoutChangingWalletOrWithdrawal() throws Exception {
        withdrawal("50000", PASSWORD, UUID.randomUUID().toString());
        Transaction tx = withdrawals().getFirst();
        mvc.perform(post("/admin/transactions/" + tx.getId() + "/resolve").with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf())).andExpect(status().isBadRequest());
        assertBalances("0", "0");
        assertThat(tx.getStatus()).isEqualTo("PENDING");
    }

    @Test void adminPaidWithdrawalCannotPayAgainOrLaterReturnMoney() throws Exception {
        withdrawal("50000", PASSWORD, UUID.randomUUID().toString());
        Transaction tx = withdrawals().getFirst();
        mvc.perform(post("/admin/transactions/" + tx.getId() + "/resolve").with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()).param("paid", "true")).andExpect(flash().attributeExists("successMessage"));
        assertBalances("0", "0");
        assertThat(tx.getStatus()).isEqualTo("COMPLETED");
        for (String repeatedDecision : new String[]{"true", "false"}) {
            mvc.perform(post("/admin/transactions/" + tx.getId() + "/resolve").with(user(admin.getUsername()).roles("ADMIN"))
                            .with(csrf()).param("paid", repeatedDecision)).andExpect(flash().attributeExists("errorMessage"));
            assertBalances("0", "0");
            assertThat(tx.getStatus()).isEqualTo("COMPLETED");
        }
        assertThat(withdrawals()).hasSize(1);
    }

    private void managerTransition(Order order, String state, String reason) throws Exception {
        var request = post("/manager/orders/update-status/" + order.getId()).with(user(manager.getUsername()).roles("MANAGER"))
                .with(csrf()).param("status", state);
        if (reason != null) request.param("cancellationReason", reason);
        mvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
    }

    private org.springframework.test.web.servlet.ResultActions withdrawal(String amount, String password, String requestId) throws Exception {
        return mvc.perform(post("/manager/wallet/withdraw").with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                .param("amount", amount).param("password", password).param("requestId", requestId)
                .param("bankName", "Acceptance bank").param("accountNumber", "123456789").param("accountHolder", "Test manager"))
                .andExpect(status().is3xxRedirection());
    }

    private int stock() { return inventories.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity(); }
    private java.util.List<Transaction> withdrawals() {
        return transactions.findAll().stream().filter(tx -> store.getId().equals(tx.getStore().getId()) && "WITHDRAWAL".equals(tx.getType())).toList();
    }
    private void assertBalances(String available, String pending) {
        em.flush(); em.refresh(store);
        assertThat(store.getEWallet()).isEqualByComparingTo(available);
        assertThat(store.getPendingBalance()).isEqualByComparingTo(pending);
    }
}
