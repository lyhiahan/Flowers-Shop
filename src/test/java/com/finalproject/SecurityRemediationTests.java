package com.finalproject;

import com.finalproject.config.*;
import com.finalproject.dto.response.NotificationEvent;
import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.security.*;
import com.finalproject.service.impl.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.util.MimeTypeUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityRemediationTests {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private MomoConfig config(String mode, String... profiles) {
        var config = new MomoConfig();
        var env = new MockEnvironment(); env.setActiveProfiles(profiles);
        ReflectionTestUtils.setField(config, "environment", env);
        ReflectionTestUtils.setField(config, "mode", mode);
        return config;
    }

    @Test void productionCannotSimulateEvenIfModeWasExplicitlySetToMock() {
        for (String mode : List.of("mock", "sandbox", "disabled")) {
            var config = config(mode, "dev", "prod");
            assertThat(config.isEnabled()).isFalse();
            if (!mode.equals("disabled")) assertThatThrownBy(config::validate).hasMessageContaining("Production");
            var service = new MomoServiceImpl(config, null, null, null, null, null, null, null, null);
            assertThatThrownBy(() -> service.simulatePayment(1L, "customer")).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> service.createPaymentUrl(null, null)).isInstanceOf(IllegalStateException.class);
        }
        assertThat(config("mock", "dev").isEnabled()).isTrue();
    }

    @Test void backendCheckoutRejectsMomoInProductionBeforeCreatingOrdersOrLockingStock() {
        var service = mock(OrderServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "momoConfig", config("disabled", "prod"));
        assertThatThrownBy(() -> service.checkout("customer", List.of(1L), PaymentMethod.MOMO))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("MoMo thử nghiệm");
    }

    @Test void bankMockIsAlsoUnavailableInProduction() {
        var config = new BankTransferConfig();
        var env = new MockEnvironment(); env.setActiveProfiles("prod");
        ReflectionTestUtils.setField(config, "environment", env);
        ReflectionTestUtils.setField(config, "mode", "mock");
        assertThat(config.isMock()).isFalse();
        assertThatThrownBy(config::validate).hasMessageContaining("Production");
    }

    @Test void passwordStampSurvivesCredentialErasureAndRejectsChangedPassword() throws Exception {
        var users = mock(UserRepository.class);
        var current = User.builder().username("customer").password("old-bcrypt-hash").role(Role.USER).isActive(true).build();
        when(users.findByUsername("customer")).thenReturn(Optional.of(current));
        var principal = new SessionUserDetails("customer", current.getPassword(), true, List.of());
        principal.eraseCredentials();
        assertThat(principal.getPassword()).isNull();
        var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        var filter = new AccountStatusFilter(users);
        var first = new AtomicBoolean();
        filter.doFilter(new MockHttpServletRequest("GET", "/customer/orders"), new MockHttpServletResponse(),
                (request, response) -> first.set(true));
        assertThat(first).isTrue();
        current.setPassword("new-bcrypt-hash");
        var request = new MockHttpServletRequest("GET", "/customer/orders");
        var session = (MockHttpSession)request.getSession();
        var response = new MockHttpServletResponse();
        var continued = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> continued.set(true));
        assertThat(continued).isFalse();
        assertThat(session.isInvalid()).isTrue();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test void realtimeJsonContainsOnlyTheNotificationContractAndNoAccountSecrets() {
        var user = User.builder().username("customer").password("SECRET_HASH").otpCode("SECRET_OTP").email("private@test.local").build();
        var notification = Notification.builder().id(1L).user(user).title("Đơn mới").content("Test").type("ORDER_NEW").relatedOrderId(2L).build();
        var converter = new JacksonJsonMessageConverter();
        var headers = new MessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON));
        var message = converter.toMessage(NotificationEvent.from(notification), headers);
        var json = new String((byte[])message.getPayload(), StandardCharsets.UTF_8);
        assertThat(json).contains("Đơn mới", "relatedOrderId").doesNotContain("password", "otpCode", "private@test.local", "SECRET", "\"user\"");
        var accountJson = new String((byte[])converter.toMessage(user, headers).getPayload(), StandardCharsets.UTF_8);
        assertThat(accountJson).doesNotContain("password", "otpCode", "SECRET");
    }
}
