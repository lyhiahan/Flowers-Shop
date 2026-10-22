package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class SessionRevocationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired OtpVerificationRepository otps;
    @Autowired PasswordEncoder encoder;
    @Autowired UserService userService;
    @Autowired AuthService auth;
    @Autowired Clock clock;
    User customer;

    @BeforeEach void setup() {
        customer = users.saveAndFlush(User.builder().username("session-revoke").email("session-revoke@test.local")
                .fullName("Session test").role(Role.USER).isActive(true).password(encoder.encode("old-password")).build());
    }

    private MockHttpSession login(String password) throws Exception {
        return (MockHttpSession)mvc.perform(post("/perform_login").with(csrf()).param("username",customer.getUsername())
                        .param("password",password)).andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);
    }

    private void rejected(MockHttpSession session) throws Exception {
        mvc.perform(get("/customer/orders").session(session)).andExpect(redirectedUrl("/login"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test void changingPasswordRevokesBothBrowsersAndNewLoginStillWorks() throws Exception {
        var first=login("old-password"); var second=login("old-password");
        mvc.perform(get("/customer/orders").session(first)).andExpect(status().isOk());
        userService.changePassword(customer.getUsername(),"old-password","new-password");
        rejected(first); rejected(second);
        mvc.perform(get("/customer/orders").session(login("new-password"))).andExpect(status().isOk());
    }

    @Test void resettingWithOtpAlsoRevokesExistingBrowserSession() throws Exception {
        var session=login("old-password");
        otps.saveAndFlush(OtpVerification.builder().user(customer).code("123456").type(OtpType.RESET_PASSWORD)
                .createdAt(LocalDateTime.now(clock)).expiredAt(LocalDateTime.now(clock).plusMinutes(5)).isUsed(false).build());
        auth.resetPassword(customer.getEmail(),"123456","reset-password");
        rejected(session);
        mvc.perform(get("/customer/orders").session(login("reset-password"))).andExpect(status().isOk());
    }
}
