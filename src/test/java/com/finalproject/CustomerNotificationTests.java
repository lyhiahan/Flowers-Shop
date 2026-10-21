package com.finalproject;

import com.finalproject.entity.Notification;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.NotificationRepository;
import com.finalproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerNotificationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notifications;
    User customer;
    User other;

    @BeforeEach void setup() {
        customer = account("notification-customer");
        other = account("notification-other");
    }

    private User account(String username) {
        return users.saveAndFlush(User.builder().username(username).email(username + "@example.test")
                .fullName("Notification customer").password("unused").role(Role.USER).isActive(true).build());
    }

    private Notification notification(User owner, String title, int minute) {
        return notifications.saveAndFlush(Notification.builder().user(owner).title(title)
                .content("Notification content " + title).type("SYSTEM").isRead(false)
                .createdAt(LocalDateTime.of(2026, 10, 20, 10, minute)).build());
    }

    @Test void bellsKeepWorkingAsLinksWhenBootstrapCannotLoad() throws Exception {
        String home = page("/");
        assertThat(home).containsPattern("<a\\b(?=[^>]*id=\"notifBellDropdown\")(?=[^>]*href=\"/notifications\")[^>]*>");
        String sharedHeader = page("/customer/orders");
        assertThat(sharedHeader).containsPattern("<a\\b(?=[^>]*id=\"floralNotifDropdown\")(?=[^>]*href=\"/notifications\")[^>]*>");
        assertThat(sharedHeader).containsPattern("<meta\\b(?=[^>]*name=\"_csrf\")(?=[^>]*content=\"[^\"]+\")[^>]*>");
    }

    @Test void notificationPageShowsCustomerContentWithoutLeakingOtherAccounts() throws Exception {
        Notification own = notification(customer, "Customer delivery update", 1);
        notification(other, "Private other notification", 2);
        String html = page("/notifications");
        assertThat(html).contains("Customer delivery update", "Notification content Customer delivery update",
                "id=\"notif_" + own.getId() + "\"").doesNotContain("Private other notification");
    }

    @Test void bellApiReturnsSixLatestNotificationsForTheSignedInCustomer() throws Exception {
        for (int i = 0; i < 8; i++) notification(customer, "Own " + i, i);
        notification(other, "Other private", 9);
        mvc.perform(get("/api/notifications/latest").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].title").value("Own 7"))
                .andExpect(jsonPath("$[5].title").value("Own 2"));
        mvc.perform(get("/api/notifications/unread-count").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(8))
                .andExpect(jsonPath("$.isManager").value(false));
    }

    @Test void customerCanReadOwnNotificationButNotSomeoneElses() throws Exception {
        Notification own = notification(customer, "Own", 1);
        Notification privateNotification = notification(other, "Other", 2);
        for (Long id : new Long[]{own.getId(), privateNotification.getId()}) {
            mvc.perform(post("/api/notifications/read/" + id)
                            .with(user(customer.getUsername()).roles("USER")).with(csrf()))
                    .andExpect(status().isOk());
        }
        assertThat(notifications.findById(own.getId()).orElseThrow().getIsRead()).isTrue();
        assertThat(notifications.findById(privateNotification.getId()).orElseThrow().getIsRead()).isFalse();
    }

    @Test void anEmptyNotificationPageStillRenders() throws Exception {
        assertThat(page("/notifications")).contains("id=\"emptyNotifAlert\"", "id=\"notificationsList\"");
    }

    @Test void vietnameseTitlesAndEmojiArePreservedInThePageAndBellApi() throws Exception {
        String title = "🌺 Đang cắm hoa đơn #30";
        notification(customer, title, 1);
        assertThat(page("/notifications")).contains(title);
        mvc.perform(get("/api/notifications/latest").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value(title));
    }

    private String page(String url) throws Exception {
        return mvc.perform(get(url).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
