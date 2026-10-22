package com.finalproject;

import com.finalproject.entity.Style;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.StyleRepository;
import com.finalproject.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AdminUiRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StyleRepository styles;
    User admin;

    @BeforeEach void setup() {
        admin = users.saveAndFlush(User.builder().username("ui-review-admin").email("ui-review@local.test")
                .fullName("UI Review Admin").password("unused").role(Role.MANAGER).roles(Set.of(Role.MANAGER, Role.ADMIN))
                .isActive(true).build());
    }

    @Test void publicProfileDoesNotExposePrivateContactDetails() throws Exception {
        var member = users.saveAndFlush(User.builder().username("public-member").fullName("Public Member")
                .email("private-email@local.test").password("private-password-hash")
                .phone("0998887776").address("Private home address 321")
                .role(Role.USER).isActive(true).build());
        String html = mvc.perform(get("/users/" + member.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("public-member", "Public Member")
                .doesNotContain(member.getAddress(), member.getEmail(), member.getPhone(), member.getPassword());
    }

    @Test void notificationPageAndBadgeUseTheSameEffectiveAdminRole() throws Exception {
        mvc.perform(get("/notifications").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/notifications"));
        mvc.perform(get("/api/notifications/unread-count").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isAdmin").value(true))
                .andExpect(jsonPath("$.isManager").value(false));
    }

    @Test void publicUsersCannotEnterAdminNotificationCenter() throws Exception {
        var member = users.saveAndFlush(User.builder().username("ui-review-member").email("member@local.test")
                .fullName("UI Review Member").password("unused").role(Role.USER).isActive(true).build());
        mvc.perform(get("/admin/notifications").with(user(member.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test void commonAdminPagesRenderOneSidebarWithTheSameMenu() throws Exception {
        String expectedMenu = null;
        for (String route : List.of("/admin/dashboard", "/admin/users", "/admin/stores",
                "/admin/products-manage", "/admin/orders-manage", "/admin/categories", "/admin/styles",
                "/admin/commissions", "/admin/vouchers", "/admin/deliveries", "/admin/user-levels",
                "/admin/store-levels", "/admin/transactions", "/admin/notifications",
                "/admin/chat", "/admin/chat/access-logs")) {
            String html = mvc.perform(get(route).with(user(admin.getUsername()).roles("ADMIN")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(html.split("id=\"adminSidebar\"", -1)).as(route).hasSize(2);
            assertThat(html.split("id=\"adminLogoutBtn\"", -1)).as(route).hasSize(2);
            assertThat(html.split("aria-label=\"Đóng menu quản trị\"", -1)).as(route).hasSize(2);
            assertThat(html.split("src=\"/js/admin-layout.js\"", -1)).as(route).hasSize(2);
            assertThat(html.split("src=\"/js/notifications-realtime.js\"", -1)).as(route).hasSize(2);
            String sidebar = html.substring(html.indexOf("<aside"), html.indexOf("</aside>") + 8);
            assertThat(sidebar.split("aria-current=\"page\"", -1)).as(route).hasSize(2);
            var links = java.util.regex.Pattern.compile("id=\"sidebar[^\"]+Link\"").matcher(sidebar);
            String menu = links.results().map(java.util.regex.MatchResult::group)
                    .collect(java.util.stream.Collectors.joining(","));
            if (expectedMenu == null) expectedMenu = menu;
            else assertThat(menu).as(route).isEqualTo(expectedMenu);
            assertThat(menu).contains("sidebarCommissionsLink", "sidebarNotificationsLink");
        }
    }

    @Test void dynamicAdminPageTitleEscapesHtml() throws Exception {
        var style = styles.saveAndFlush(Style.builder().name("</title><script>alert(1)</script>").build());
        String html = mvc.perform(get("/admin/styles/" + style.getId() + "/values")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).doesNotContain("<script>alert(1)</script>")
                .contains("&lt;/title&gt;&lt;script&gt;alert(1)&lt;/script&gt;");
    }
}
