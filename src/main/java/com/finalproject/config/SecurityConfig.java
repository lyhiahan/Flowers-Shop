package com.finalproject.config;

import com.finalproject.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**", "/customer/profile/send-password-otp", "/customer/send-password-otp"))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD, jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager", "/manager/**", "/vendor", "/vendor/**").hasAnyRole("ADMIN", "MANAGER", "VENDOR")
                .requestMatchers("/shipper", "/shipper/**").hasRole("SHIPPER")
                .requestMatchers("/staff", "/staff/**").hasAnyRole("ADMIN", "MANAGER", "VENDOR", "STAFF")
                .requestMatchers("/ws", "/ws/**").hasAnyRole("ADMIN", "MANAGER", "VENDOR", "STAFF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/inventory/check").permitAll()
                .requestMatchers("/customer/**", "/cart", "/cart/**", "/checkout", "/checkout/**", "/following").hasAnyRole("CUSTOMER", "ADMIN", "MANAGER", "VENDOR", "STAFF", "SHIPPER")
                .requestMatchers("/", "/products/**", "/product/**", "/stores/**", "/users/**", "/login", "/register", "/verify-otp", "/resend-otp", "/forgot-password", "/reset-password", "/api/public/**", "/ws/**", "/css/**", "/js/**", "/images/**", "/upload/**", "/error", "/favicon.ico", "/webjars/**").permitAll()
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
