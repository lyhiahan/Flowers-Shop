package com.finalproject;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.service.NotificationNavigationResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationNavigationResolverTest {

    private final NotificationNavigationResolver resolver = new NotificationNavigationResolver();

    @Test
    @DisplayName("Admin with valid relatedOrderId resolves to /admin/orders/{id}")
    void admin_withOrderId() {
        String url = resolver.resolveTargetUrl(Role.ADMIN, 10L, 4L);
        assertThat(url).isEqualTo("/admin/orders/4");
    }

    @Test
    @DisplayName("Admin without relatedOrderId resolves to /admin/notifications#notif_{id}")
    void admin_withoutOrderId() {
        String url = resolver.resolveTargetUrl(Role.ADMIN, 10L, null);
        assertThat(url).isEqualTo("/admin/notifications#notif_10");
    }

    @Test
    @DisplayName("Admin with null notificationId and null orderId resolves to /admin/notifications safely")
    void admin_nullIds() {
        String url = resolver.resolveTargetUrl(Role.ADMIN, null, null);
        assertThat(url).isEqualTo("/admin/notifications");
    }

    @Test
    @DisplayName("Admin with non-positive orderId resolves to /admin/notifications#notif_{id}")
    void admin_invalidOrderId() {
        String url = resolver.resolveTargetUrl(Role.ADMIN, 12L, -1L);
        assertThat(url).isEqualTo("/admin/notifications#notif_12");
    }

    @Test
    @DisplayName("Manager with valid relatedOrderId resolves to /manager/orders/{id}")
    void manager_withOrderId() {
        String url = resolver.resolveTargetUrl(Role.MANAGER, 20L, 88L);
        assertThat(url).isEqualTo("/manager/orders/88");
    }

    @Test
    @DisplayName("Manager without relatedOrderId resolves to /manager/notifications#notif_{id}")
    void manager_withoutOrderId() {
        String url = resolver.resolveTargetUrl(Role.MANAGER, 20L, null);
        assertThat(url).isEqualTo("/manager/notifications#notif_20");
    }

    @Test
    @DisplayName("User with valid relatedOrderId resolves to /customer/orders/detail/{id}")
    void user_withOrderId() {
        String url = resolver.resolveTargetUrl(Role.USER, 30L, 99L);
        assertThat(url).isEqualTo("/customer/orders/detail/99");
    }

    @Test
    @DisplayName("User without relatedOrderId resolves to /notifications#notif_{id}")
    void user_withoutOrderId() {
        String url = resolver.resolveTargetUrl(Role.USER, 30L, null);
        assertThat(url).isEqualTo("/notifications#notif_30");
    }

    @Test
    @DisplayName("Authentication with both ROLE_ADMIN and ROLE_MANAGER prioritizes ADMIN")
    void multiRole_adminPrioritized() {
        var auth = new UsernamePasswordAuthenticationToken(
                "superadmin", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        String urlWithOrder = resolver.resolveTargetUrl(auth, 1L, 50L);
        assertThat(urlWithOrder).isEqualTo("/admin/orders/50");

        String urlWithoutOrder = resolver.resolveTargetUrl(auth, 1L, null);
        assertThat(urlWithoutOrder).isEqualTo("/admin/notifications#notif_1");
    }

    @Test
    @DisplayName("User entity parameter safely unwraps role and handles null user")
    void userEntity_overload() {
        User adminUser = User.builder().id(1L).role(Role.ADMIN).build();
        assertThat(resolver.resolveTargetUrl(adminUser, 5L, 10L)).isEqualTo("/admin/orders/10");

        User managerUser = User.builder().id(2L).role(Role.MANAGER).build();
        assertThat(resolver.resolveTargetUrl(managerUser, 6L, null)).isEqualTo("/manager/notifications#notif_6");

        // User with primary role MANAGER but roles set containing ADMIN
        User multiRoleAdmin = User.builder().id(3L).role(Role.MANAGER)
                .roles(java.util.Set.of(Role.MANAGER, Role.ADMIN)).build();
        assertThat(resolver.resolveTargetUrl(multiRoleAdmin, 7L, 25L)).isEqualTo("/admin/orders/25");

        // User with primary role USER but roles set containing MANAGER
        User multiRoleManager = User.builder().id(4L).role(Role.USER)
                .roles(java.util.Set.of(Role.USER, Role.MANAGER)).build();
        assertThat(resolver.resolveTargetUrl(multiRoleManager, 8L, 35L)).isEqualTo("/manager/orders/35");

        assertThat(resolver.resolveTargetUrl((User) null, 9L, 15L)).isEqualTo("/customer/orders/detail/15");
    }

    @Test
    @DisplayName("Unauthenticated or anonymous context safely defaults to USER")
    void unauthenticated_defaultsToUser() {
        assertThat(resolver.resolveTargetUrl((org.springframework.security.core.Authentication) null, 9L, 100L))
                .isEqualTo("/customer/orders/detail/100");
    }
}
