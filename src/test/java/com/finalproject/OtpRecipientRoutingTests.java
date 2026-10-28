package com.finalproject;

import com.finalproject.controller.customer.ProfileController;
import com.finalproject.dto.request.RegisterRequest;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import com.finalproject.service.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpRecipientRoutingTests {
    private final UserRepository users = mock(UserRepository.class);
    private final OtpVerificationRepository otps = mock(OtpVerificationRepository.class);
    private final EmailService mail = mock(EmailService.class);
    private AuthService auth() {
        var validator = mock(Validator.class);
        when(validator.validate(any(RegisterRequest.class))).thenReturn(Set.of());
        return new AuthService(users, otps, mock(CartRepository.class), mail,
                mock(PasswordEncoder.class), validator, mock(EntityManager.class), java.time.Clock.system(java.time.ZoneId.of("Asia/Bangkok")));
    }
    private RegisterRequest request(String email) {
        var request = new RegisterRequest();
        request.setUsername("mail_recipient"); request.setEmail(email); request.setPassword("safe-password");
        request.setFullName("Mail Recipient"); request.setPhone("0901234567");
        request.setProvince("TP HCM"); request.setDistrict("Quận 1"); request.setDetailAddress("123 Test");
        return request;
    }
    private ProfileController profile() {
        return new ProfileController(users, mock(UserService.class),
                mock(ProductService.class), mock(UserFollowStoreRepository.class), mock(StoreRepository.class),
                mock(CartService.class), otps, mail);
    }
    private User user() {
        return User.builder().id(1L).username("customer_name").email("owner+flowers@example.test").role(Role.USER).build();
    }

    @Test void registrationUsesExactlyNewCustomersNormalizedEmail() {
        var auth = auth();
        String code = auth.registerCustomer(request(" New.Customer+shop@outlook.com "));
        verify(mail).sendOtpEmail("new.customer+shop@outlook.com", code);
        assertThat(code).matches("[0-9]{6}");
    }

    @Test void resendUsesThePendingRegistrationEmailNotAFixedSenderMailbox() {
        var auth = auth();
        auth.registerCustomer(request("newcustomer@gmail.com"));
        clearInvocations(mail);
        String code = auth.resendOtp("newcustomer@gmail.com");
        verify(mail).sendOtpEmail("newcustomer@gmail.com", code);
    }

    @Test void forgottenPasswordByUsernameSendsToAccountEmailNotTheUsername() {
        var user = user();
        when(users.findByEmail(user.getUsername())).thenReturn(Optional.empty());
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(users.findLockedById(user.getId())).thenReturn(Optional.of(user));
        String code = auth().forgotPassword(user.getUsername());
        verify(mail).sendOtpEmail(user.getEmail(), code);
    }

    @Test void profileOtpGoesToAuthenticatedOwnersEmail() {
        var user = user();
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        var response = profile().sendPasswordOtp(user::getUsername, "OWNER+FLOWERS@example.test");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(mail).sendOtpEmail(eq(user.getEmail()), matches("[0-9]{6}"));
    }

    @Test void changingRequestEmailCannotDivertAccountOtpToSomebodyElse() {
        var user = user();
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        var response = profile().sendPasswordOtp(user::getUsername, "attacker@example.test");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(mail, otps);
    }
}
