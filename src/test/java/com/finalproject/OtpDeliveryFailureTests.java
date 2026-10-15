package com.finalproject;

import com.finalproject.controller.customer.ProfileController;
import com.finalproject.dto.request.RegisterRequest;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import com.finalproject.service.impl.AuthServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpDeliveryFailureTests {
    private final UserRepository users = mock(UserRepository.class);
    private final OtpVerificationRepository otps = mock(OtpVerificationRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final CartRepository carts = mock(CartRepository.class);
    private AuthServiceImpl auth() {
        var validator = mock(Validator.class);
        when(validator.validate(any(RegisterRequest.class))).thenReturn(Set.of());
        return new AuthServiceImpl(users, otps, carts, email, mock(PasswordEncoder.class), validator, mock(EntityManager.class));
    }
    private RegisterRequest request() {
        var request = new RegisterRequest();
        request.setUsername("mail_failure"); request.setEmail("mail-failure@example.test");
        request.setPassword("safe-password"); request.setFullName("Mail Failure"); request.setPhone("0901234567");
        request.setProvince("TP HCM"); request.setDistrict("Quận 1"); request.setDetailAddress("123 Test");
        return request;
    }

    @Test void failedRegistrationEmailDoesNotLeavePendingAccountAndCanRetry() {
        var auth = auth();
        doThrow(new IllegalStateException("SMTP unavailable")).doNothing().when(email).sendOtpEmail(anyString(), anyString());
        var request = request();
        assertThatThrownBy(() -> auth.registerCustomer(request)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không thể gửi email");
        verifyNoInteractions(carts);
        var code = auth.registerCustomer(request);
        assertThat(code).matches("[0-9]{6}");
        assertThat(auth.verifyOtp(request.getEmail(), code)).isTrue();
    }

    @Test void failedResendPreservesPreviouslyDeliveredOtp() {
        var auth = auth();
        var request = request();
        var original = auth.registerCustomer(request);
        doThrow(new IllegalStateException("SMTP unavailable")).when(email).sendOtpEmail(anyString(), anyString());
        assertThatThrownBy(() -> auth.resendOtp(request.getEmail())).hasMessageContaining("Mã cũ vẫn còn hiệu lực");
        assertThat(auth.verifyOtp(request.getEmail(), original)).isTrue();
    }

    @Test void failedPasswordOtpReturnsServiceUnavailableAndRemovesUnsentCode() {
        var user = User.builder().id(1L).username("otp_user").email("otp-user@example.test").role(Role.USER).build();
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("SMTP unavailable")).when(email).sendOtpEmail(anyString(), anyString());
        var controller = new ProfileController(users, mock(UserService.class),
                mock(ProductService.class), mock(UserFollowStoreRepository.class), mock(StoreRepository.class),
                mock(CartService.class), otps, email);
        var response = controller.sendPasswordOtp(user::getUsername, user.getEmail());
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(((Map<?, ?>) response.getBody()).get("success")).isEqualTo(false);
        verify(otps).delete(any(com.finalproject.entity.OtpVerification.class));
    }
}
