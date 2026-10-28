package com.finalproject.config;

import com.finalproject.service.CustomUserDetailsService;
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

/**
 * Session authentication for Thymeleaf and same-origin AJAX without Bearer headers.
 * JwtSecurityConfig handles explicit JWT API requests in a separate stateless chain.
 * Servlet HttpSession authenticates browser requests via the session cookie.
 * Merely declaring spring-session-jdbc does not enable JDBC session persistence.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final com.finalproject.repository.UserRepository userRepository;

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

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(new com.finalproject.security.AccountStatusFilter(userRepository),
                    org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                "/ws",
                "/ws/**",
                "/register",
                "/verify-otp",
                "/resend-otp",
                "/forgot-password",
                "/reset-password",
                "/checkout/momo/ipn"
            ))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD, jakarta.servlet.DispatcherType.ERROR).permitAll()
                // Quyền Admin
                .requestMatchers("/admin", "/admin/**", "/api/admin/**").hasRole("ADMIN")
                // Quyền Quản lý / Vendor
                .requestMatchers("/manager", "/manager/**", "/vendor", "/vendor/**", "/api/manager/**", "/api/vendor/**").hasAnyRole("ADMIN", "MANAGER")
                // Retired delivery portal must not accept old sessions.
                .requestMatchers("/shipper", "/shipper/**").denyAll()
                // Quyền Staff
                .requestMatchers("/staff", "/staff/**", "/api/staff/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                // Endpoint WebSocket SockJS
                .requestMatchers("/ws", "/ws/**", "/ws-chat", "/api/chat/**").authenticated()
                // MoMo Callback & IPN Webhook (cho phép chuyển hướng và webhook không bị chặn)
                .requestMatchers("/checkout/momo/callback", "/checkout/momo/ipn").permitAll()
                .requestMatchers("/demo-payment/**").permitAll()
                // Quyền Khách hàng
                .requestMatchers("/customer/**", "/cart", "/cart/**", "/checkout", "/checkout/**", "/following", "/api/customer/**").hasRole("USER")
                // Các tài nguyên công khai
                .requestMatchers(
                    "/", "/products/**", "/product/**", "/search", "/search/**", "/stores/**", "/users/**",
                    "/login", "/register", "/verify-otp", "/resend-otp", "/forgot-password", "/reset-password",
                        "/api/inventory/check", "/api/public/**",
                    "/css/**", "/js/**", "/images/**", "/upload/**", "/error", "/favicon.ico", "/webjars/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    org.springframework.security.core.Authentication auth =
                            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
                        String uri = request.getRequestURI();
                        if (uri != null && (uri.startsWith("/cart") || uri.startsWith("/checkout") || uri.startsWith("/customer") || uri.startsWith("/following"))) {
                            if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                                    || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"))) {
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write("{\"success\":false,\"message\":\"Tài khoản Quản trị viên không thể thực hiện chức năng mua hàng hoặc truy cập giỏ hàng.\"}");
                                return;
                            }
                            response.sendRedirect("/admin/dashboard?error=admin_shopping_denied");
                            return;
                        }
                    }
                    if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                            || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"))) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này.\"}");
                        return;
                    }
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                })
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
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
