package com.finalproject.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String CUSTOMER_RETURN_TO = "customerReturnTo";

    public static String safeCustomerTarget(String target) {
        if (target == null || target.chars().anyMatch(Character::isISOControl)) return null;
        try {
            var uri = java.net.URI.create(target);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawFragment() != null) return null;
            String path = uri.getRawPath();
            return path != null && path.matches("/(?:product/[0-9]+|products(?:/[0-9]+)?|cart|checkout)") ? target : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        var session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(CustomAuthenticationFailureHandler.ATTEMPTED_USERNAME);
            session.removeAttribute(org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION);
        }
        String customerTarget = session == null ? null : safeCustomerTarget((String) session.getAttribute(CUSTOMER_RETURN_TO));
        if (session != null) session.removeAttribute(CUSTOMER_RETURN_TO);
        var cache = new org.springframework.security.web.savedrequest.HttpSessionRequestCache();
        var saved = cache.getRequest(request, response);
        if (customerTarget == null && saved != null && "GET".equals(saved.getMethod())) {
            var uri = java.net.URI.create(saved.getRedirectUrl());
            String query = uri.getRawQuery() == null ? "" : java.util.Arrays.stream(uri.getRawQuery().split("&"))
                    .filter(part -> !part.equals("continue")).collect(java.util.stream.Collectors.joining("&"));
            customerTarget = safeCustomerTarget(uri.getRawPath() + (query.isEmpty() ? "" : "?" + query));
        }
        cache.removeRequest(request, response);
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_ADMIN")) {
            response.sendRedirect("/admin/dashboard");
        } else if (roles.contains("ROLE_MANAGER")) {
            response.sendRedirect("/manager/dashboard");
        } else if (roles.contains("ROLE_STAFF")) {
            response.sendRedirect("/staff/orders");
        } else {
            response.sendRedirect(customerTarget == null ? "/" : customerTarget);
        }
    }
}
