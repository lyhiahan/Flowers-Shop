package com.finalproject;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import com.finalproject.service.JwtTokenService;
import com.finalproject.config.JwtProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:jwt-tests;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtRefreshTokenRepository refreshTokens;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @Autowired JwtProperties properties;
    User customer;
    record Pair(String access, String refresh) {}

    @BeforeEach
    void account() {
        String name = "jwt_" + UUID.randomUUID().toString().replace("-", "");
        customer = users.saveAndFlush(User.builder().username(name).fullName("JWT Customer")
                .email(name + "@test.local").password(passwords.encode("test-password"))
                .role(Role.USER).isActive(true).build());
    }

    Pair login() throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + customer.getUsername() + "\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900)).andReturn().getResponse().getContentAsString();
        return pair(json);
    }

    static Pair pair(String json) {
        return new Pair(field(json, "accessToken"), field(json, "refreshToken"));
    }

    static String field(String json, String name) {
        var match = java.util.regex.Pattern.compile("\"" + name + "\":\"([^\"]+)\"").matcher(json);
        assertThat(match.find()).isTrue();
        return match.group(1);
    }

    String refreshBody(String raw) { return "{\"refreshToken\":\"" + raw + "\"}"; }

    @Test void jwtWorksWithoutHttpSessionAndNeverExposesPassword() throws Exception {
        Pair pair = login();
        assertThat(decoder.decode(pair.access()).getSubject()).isEqualTo(customer.getUsername());
        var result = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.access()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(customer.getUsername()))
                .andExpect(jsonPath("$.roles[0]").value("USER")).andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(refreshTokens.findAll().stream().map(t -> t.getTokenHash())).doesNotContain(pair.refresh());
    }

    @Test void invalidPasswordAndDisabledAccountCannotLogin() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + customer.getUsername() + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        customer.setIsActive(false); users.saveAndFlush(customer);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + customer.getEmail() + "\",\"password\":\"test-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void sessionAuthenticationCannotReplaceBearerToken() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                customer.getUsername(), null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void malformedAndTamperedTokensAreRejected() throws Exception {
        Pair pair = login();
        String[] segments = pair.access().split("\\.");
        segments[1] = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"admin\"}".getBytes());
        for (String invalid : List.of("invalid", String.join(".", segments))) {
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test void expiredWrongIssuerAndWrongAudienceTokensAreRejected() throws Exception {
        String sid = decoder.decode(login().access()).getClaimAsString("sid");
        for (int scenario = 0; scenario < 3; scenario++) {
            Instant now = Instant.now();
            var claims = JwtClaimsSet.builder().subject(customer.getUsername())
                    .issuer(scenario == 1 ? "wrong" : properties.issuer())
                    .audience(List.of(scenario == 2 ? "wrong" : properties.audience()))
                    .issuedAt(now.minusSeconds(600)).notBefore(now.minusSeconds(600))
                    .expiresAt(scenario == 0 ? now.minusSeconds(120) : now.plusSeconds(600))
                    .claim("token_type", "access").claim("sid", sid).build();
            String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }

    @Test void refreshRotatesAndReplayRevokesEntireFamily() throws Exception {
        Pair first = login();
        String json = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(first.refresh()))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Pair second = pair(json);
        assertThat(second.refresh()).isNotEqualTo(first.refresh());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + second.access())).andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(first.refresh()))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + second.access())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(second.refresh()))).andExpect(status().isUnauthorized());
    }

    @Test void logoutRevokesAccessAndRefreshTokens() throws Exception {
        Pair pair = login();
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + pair.access())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.access())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(pair.refresh()))).andExpect(status().isUnauthorized());
    }

    @Test void disablingAccountImmediatelyBlocksExistingTokens() throws Exception {
        Pair pair = login();
        customer.setAccountDisabledByAdmin(true); users.saveAndFlush(customer);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.access())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(pair.refresh()))).andExpect(status().isUnauthorized());
    }

    @Test void passwordChangeImmediatelyRevokesExistingTokens() throws Exception {
        Pair pair = login();
        customer.setPassword(passwords.encode("changed-password")); users.saveAndFlush(customer);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.access())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(pair.refresh()))).andExpect(status().isUnauthorized());
    }

    @Test void roleChangesApplyToAlreadyIssuedToken() throws Exception {
        Pair pair = login();
        customer.setRole(Role.ADMIN); customer.setRoles(new HashSet<>()); users.saveAndFlush(customer);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.access()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.roles.length()").value(1));
        mvc.perform(get("/api/customer/pickup-check").param("storeId", "1")
                .header("Authorization", "Bearer " + pair.access())).andExpect(status().isForbidden());
    }

    @Test void bearerTokenWorksWithExistingChatApi() throws Exception {
        Pair pair = login();
        mvc.perform(get("/api/chat/context").header("Authorization", "Bearer " + pair.access()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(customer.getId()));
    }

    @Test void invalidJsonReturnsApiError() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_request"));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("x".repeat(64)))).andExpect(status().isUnauthorized());
    }

    @Test void sessionNotificationWritesRequireCsrfWhileBearerApiDoesNotUseCookies() throws Exception {
        mvc.perform(post("/api/notifications/read-all").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/notifications/read-all").with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().isOk());
        Pair pair = login();
        mvc.perform(post("/api/notifications/read-all").header("Authorization", "Bearer " + pair.access()))
                .andExpect(status().isOk());
    }
}
