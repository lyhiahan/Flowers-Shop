package com.finalproject.config;

import com.finalproject.security.JwtAuthenticationFilter;
import com.finalproject.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình bảo mật hai lớp:
 *
 * 1. filterChain (Thymeleaf / session):
 *    - formLogin + CSRF bật đầy đủ
 *    - Session-based authentication giữ nguyên
 *    - Áp dụng cho tất cả URL ngoại trừ /api/**
 *
 * 2. apiFilterChain (REST API):
 *    - Stateless, không session, không CSRF (vì dùng Bearer token)
 *    - JwtAuthenticationFilter chạy trước
 *    - Chỉ áp dụng cho /api/**
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ─── Filter chain cho API (/api/**) ──────────────────────────────────────

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            // Chỉ áp dụng cho /api/**
            .securityMatcher("/api/**")
            // API dùng JWT → không cần session, không cần CSRF
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Đăng nhập và public API không cần xác thực
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/inventory/check").permitAll()
                // Admin API
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Manager/Vendor API
                .requestMatchers("/api/manager/**", "/api/vendor/**").hasAnyRole("ADMIN", "MANAGER", "VENDOR")
                // Customer API (đã đăng nhập)
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                // Tất cả /api/** còn lại yêu cầu đăng nhập
                .anyRequest().authenticated()
            )
            // JWT filter chạy trước filter xác thực mặc định
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Trả JSON 401 khi chưa xác thực (thay vì redirect về /login)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"Yêu cầu xác thực. Vui lòng đăng nhập và cung cấp Bearer token.\",\"status\":401}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"Không có quyền truy cập tài nguyên này.\",\"status\":403}");
                })
            );

        return http.build();
    }

    // ─── Filter chain cho Thymeleaf (session-based) ───────────────────────────

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                "/ws/**",
                "/register",
                "/verify-otp",
                "/resend-otp",
                "/forgot-password",
                "/reset-password"
                // Lưu ý: /api/** được xử lý ở apiFilterChain với CSRF disable riêng
            ))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD, jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager", "/manager/**", "/vendor", "/vendor/**").hasAnyRole("ADMIN", "MANAGER", "VENDOR")
                .requestMatchers("/shipper", "/shipper/**").hasRole("SHIPPER")
                .requestMatchers("/staff", "/staff/**").hasAnyRole("ADMIN", "MANAGER", "VENDOR", "STAFF")
                .requestMatchers("/ws", "/ws/**").hasAnyRole("ADMIN", "MANAGER", "VENDOR", "STAFF")
                .requestMatchers("/customer/**", "/cart", "/cart/**", "/checkout", "/checkout/**", "/following").hasRole("CUSTOMER")
                .requestMatchers(
                    "/", "/products/**", "/product/**", "/stores/**", "/users/**",
                    "/login", "/register", "/verify-otp", "/resend-otp", "/forgot-password", "/reset-password",
                    "/css/**", "/js/**", "/images/**", "/upload/**", "/error", "/favicon.ico", "/webjars/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
