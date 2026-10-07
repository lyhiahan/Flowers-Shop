package com.finalproject.security;

import com.finalproject.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Prevents a previously authenticated session from bypassing account retirement or locking. */
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            var user = users.findByUsername(authentication.getName()).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getIsActive()) || user.isRetiredDeliveryAccount()
                    || Boolean.TRUE.equals(user.getAccountDisabledByAdmin())) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản đã bị khóa hoặc vô hiệu hóa.");
                return;
            }
            var roles = new java.util.HashSet<org.springframework.security.core.GrantedAuthority>();
            if (user.hasRole(com.finalproject.enums.Role.ADMIN)) {
                roles.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
            } else {
                roles.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
                if (user.hasRole(com.finalproject.enums.Role.MANAGER))
                    roles.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MANAGER"));
            }
            if (!roles.equals(new java.util.HashSet<>(authentication.getAuthorities()))) {
                var refreshed = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        authentication.getPrincipal(), authentication.getCredentials(), roles);
                refreshed.setDetails(authentication.getDetails());
                SecurityContextHolder.getContext().setAuthentication(refreshed);
            }
        }
        chain.doFilter(request, response);
    }
}
