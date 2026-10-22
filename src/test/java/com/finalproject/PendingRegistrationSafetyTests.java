package com.finalproject;

import com.finalproject.dto.PendingRegistration;
import com.finalproject.dto.request.RegisterRequest;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import com.finalproject.service.impl.AuthServiceImpl;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PendingRegistrationSafetyTests {
    @Test void pendingCacheRetainsOnlyAHashAndDetachedFieldsWithAnExplicitClock() {
        var validator=mock(Validator.class);
        when(validator.validate(any(RegisterRequest.class))).thenReturn(Set.of());
        var encoder=new BCryptPasswordEncoder();
        var clock=Clock.fixed(Instant.parse("2026-10-21T17:01:00Z"),ZoneId.of("Asia/Bangkok"));
        var auth=new AuthServiceImpl(mock(UserRepository.class),mock(OtpVerificationRepository.class),mock(CartRepository.class),
                mock(EmailService.class),encoder,validator,mock(EntityManager.class),clock);
        var request=new RegisterRequest();
        request.setUsername("pending-user"); request.setEmail("pending@test.local"); request.setPassword("safe-password");
        request.setPhone("0901234567"); request.setAddress("123 Test street");
        auth.registerCustomer(request);
        @SuppressWarnings("unchecked") var cache=(Map<String,PendingRegistration>)ReflectionTestUtils.getField(auth,"pendingRegistrations");
        var pending=cache.get("pending@test.local");
        assertThat(pending.getRequest().getPassword()).isNull();
        assertThat(encoder.matches("safe-password",pending.getPasswordHash())).isTrue();
        assertThat(request.getPassword()).isEqualTo("safe-password");
        request.setUsername("changed-after-register");
        assertThat(pending.getRequest().getUsername()).isEqualTo("pending-user");
        assertThat(pending.getExpiredAt()).isEqualTo(LocalDateTime.now(clock).plusMinutes(5));
    }
}
