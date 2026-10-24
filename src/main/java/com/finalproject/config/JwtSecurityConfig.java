package com.finalproject.config;

import com.finalproject.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/** Header-based API clients use JWT. Existing browser requests retain session/CSRF. */
@Configuration
@RequiredArgsConstructor
public class JwtSecurityConfig {
    private final JwtTokenService tokens;
    private final JwtDecoder decoder;

    @Bean
    @Order(1)
    public SecurityFilterChain jwtApiChain(HttpSecurity http) throws Exception {
        http.securityMatcher(request -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String header = request.getHeader("Authorization");
            return path.equals("/api/auth") || path.startsWith("/api/auth/")
                    || (path.startsWith("/api/") && header != null
                    && header.regionMatches(true, 0, "Bearer", 0, 6));
        })
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(s -> s.securityContextRepository(
                    new org.springframework.security.web.context.NullSecurityContextRepository()))
            .requestCache(c -> c.disable())
            .csrf(c -> c.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**", "/api/vendor/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/staff/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                .requestMatchers("/api/customer/**").hasRole("USER")
                .requestMatchers("/api/inventory/check", "/api/public/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder)
                .jwtAuthenticationConverter(tokens::authenticate)))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, failure) -> {
                    res.setStatus(401);
                    res.setHeader("WWW-Authenticate", "Bearer");
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"unauthorized\"}");
                })
                .accessDeniedHandler((req, res, failure) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"access_denied\"}");
                }));
        return http.build();
    }
}
