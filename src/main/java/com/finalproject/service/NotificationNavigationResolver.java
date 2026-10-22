package com.finalproject.service;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Backend-only navigation resolver for notifications based on authenticated recipient context.
 * Strict role resolution: ADMIN is always prioritized over MANAGER.
 * Never trusts client-supplied roles or arbitrary redirects.
 */
@Component
public class NotificationNavigationResolver {

    public String resolveTargetUrl(Role role, Long notificationId, Long relatedOrderId) {
        boolean hasOrder = relatedOrderId != null && relatedOrderId > 0;
        String notifAnchor = (notificationId != null && notificationId > 0) ? "#notif_" + notificationId : "";

        if (role == Role.ADMIN) {
            return hasOrder ? "/admin/orders/" + relatedOrderId : "/admin/notifications" + notifAnchor;
        } else if (role == Role.MANAGER) {
            return hasOrder ? "/manager/orders/" + relatedOrderId : "/manager/notifications" + notifAnchor;
        } else {
            return hasOrder ? "/customer/orders/detail/" + relatedOrderId : "/notifications" + notifAnchor;
        }
    }

    public Role resolveRole(User user) {
        if (user == null) {
            return Role.USER;
        }
        if (user.hasRole(Role.ADMIN)) {
            return Role.ADMIN;
        }
        if (user.hasRole(Role.MANAGER)) {
            return Role.MANAGER;
        }
        return Role.USER;
    }

    public String resolveTargetUrl(User user, Long notificationId, Long relatedOrderId) {
        Role role = resolveRole(user);
        return resolveTargetUrl(role, notificationId, relatedOrderId);
    }

    public String resolveTargetUrl(Authentication authentication, Long notificationId, Long relatedOrderId) {
        Role role = resolveRole(authentication);
        return resolveTargetUrl(role, notificationId, relatedOrderId);
    }

    public Role resolveRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Role.USER;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return Role.USER;
        }
        boolean isAdmin = authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return Role.ADMIN;
        }
        boolean isManager = authorities.stream().anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()));
        if (isManager) {
            return Role.MANAGER;
        }
        return Role.USER;
    }
}
