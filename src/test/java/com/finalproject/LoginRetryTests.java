package com.finalproject;

import com.finalproject.config.CustomAuthenticationFailureHandler;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import java.util.Collections;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginRetryTests {
    private static final String FAILED_PASSWORD = "wrong-secret-not-to-retain-7737";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @BeforeEach void account() {
        users.saveAndFlush(User.builder().username("keep-me").email("remembered@example.test")
                .fullName("Login retry account").password(passwords.encode("correct-retry-password"))
                .role(Role.USER).isActive(true).build());
    }

    void fail(MockHttpSession session, String identifier) throws Exception {
        mvc.perform(post("/perform_login").session(session).with(anonymous()).with(csrf())
                        .param("username", identifier).param("password", FAILED_PASSWORD))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());
    }

    String page(MockHttpSession session, String identifier) throws Exception {
        String html = mvc.perform(get("/login").session(session).param("error", "true"))
                .andExpect(status().isOk()).andExpect(model().attribute("loginUsername", identifier))
                .andExpect(model().attribute("loginFailed", true)).andReturn().getResponse().getContentAsString();
        var username = Pattern.compile("<input\\b(?=[^>]*id=\"usernameInput\")(?=[^>]*value=\"([^\"]*)\")[^>]*>").matcher(html);
        assertThat(username.find()).isTrue();
        assertThat(username.group(1)).isEqualTo(HtmlUtils.htmlEscape(identifier));
        var password = Pattern.compile("<input\\b(?=[^>]*id=\"passwordInput\")[^>]*>").matcher(html);
        assertThat(password.find()).isTrue();
        assertThat(password.group()).doesNotContain("value=").contains("autocomplete=\"off\"");
        assertThat(html).doesNotContain(FAILED_PASSWORD).contains("/js/login-retry.js");
        return html;
    }

    @ParameterizedTest
    @ValueSource(strings = {"keep-me", "remembered@example.test", "wrong.username", "other@gmail.com", "  wrong.identity  "})
    void failedLoginDisplaysExactlySubmittedUsernameOrEmailWithoutRetainingPassword(String identifier) throws Exception {
        var session = new MockHttpSession();
        fail(session, identifier);
        assertThat(session.getAttribute(CustomAuthenticationFailureHandler.ATTEMPTED_USERNAME)).isEqualTo(identifier);
        page(session, identifier);
        String values = Collections.list(session.getAttributeNames()).stream()
                .map(name -> String.valueOf(session.getAttribute(name))).collect(java.util.stream.Collectors.joining("\n"));
        assertThat(values).doesNotContain(FAILED_PASSWORD);
    }

    @Test void maliciousIdentifierIsEscapedRatherThanRenderedAsHtml() throws Exception {
        String identifier = "\"><script>alert('retry')</script>&";
        var session = new MockHttpSession();
        fail(session, identifier);
        assertThat(page(session, identifier)).doesNotContain("<script>alert('retry')</script>");
    }

    @Test void refreshingErrorPageKeepsLastAttemptAndAnotherFailureReplacesIt() throws Exception {
        var session = new MockHttpSession();
        fail(session, "first@example.test");
        page(session, "first@example.test");
        page(session, "first@example.test");
        fail(session, "second-wrong-name");
        page(session, "second-wrong-name");
    }

    @Test void differentBrowserSessionsNeverShareAnAttemptedIdentifier() throws Exception {
        var first = new MockHttpSession(); var second = new MockHttpSession();
        fail(first, "first@example.test"); fail(second, "second@example.test");
        page(first, "first@example.test"); page(second, "second@example.test");
    }

    @Test void freshLoginPageClearsPreviousAttemptAndAllowsNormalAutofill() throws Exception {
        var session = new MockHttpSession();
        fail(session, "first@example.test");
        mvc.perform(get("/login").session(session)).andExpect(status().isOk())
                .andExpect(model().attribute("loginUsername", "")).andExpect(model().attribute("loginFailed", false))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("autocomplete=\"username\"")));
        assertThat(session.getAttribute(CustomAuthenticationFailureHandler.ATTEMPTED_USERNAME)).isNull();
    }

    @Test void successfulLoginClearsAttemptAndPreservesCustomerRedirect() throws Exception {
        var session = new MockHttpSession();
        fail(session, "old-attempt");
        mvc.perform(post("/perform_login").session(session).with(csrf())
                        .param("username", "remembered@example.test").param("password", "correct-retry-password"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("keep-me"));
        assertThat(session.getAttribute(CustomAuthenticationFailureHandler.ATTEMPTED_USERNAME)).isNull();
        mvc.perform(get("/customer/orders").session(session)).andExpect(status().isOk())
                .andExpect(authenticated().withUsername("keep-me"));
    }

    @Test void oversizedAttemptIsBoundedAndDoesNotEnterRedirectUrl() throws Exception {
        var session = new MockHttpSession();
        fail(session, "a".repeat(2000));
        page(session, "a".repeat(254));
    }

    @Test void missingIdentifierDoesNotFallBackToAnySavedOrDemoAccount() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post("/perform_login").session(session).with(anonymous()).with(csrf())
                        .param("password", FAILED_PASSWORD)).andExpect(status().is3xxRedirection());
        page(session, "");
    }

    @Test void csrfFailureDoesNotStoreIdentifierAsAnAuthenticationAttempt() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post("/perform_login").session(session).with(anonymous())
                        .param("username", "not-a-login-attempt").param("password", FAILED_PASSWORD))
                .andExpect(status().isForbidden());
        assertThat(session.getAttribute(CustomAuthenticationFailureHandler.ATTEMPTED_USERNAME)).isNull();
    }
}
