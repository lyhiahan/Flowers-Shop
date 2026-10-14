package com.finalproject;

import com.finalproject.controller.common.NotificationController;
import com.finalproject.entity.Notification;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.repository.NotificationRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.NotificationService;
import com.finalproject.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceAndControllerTests {

    @Nested
    @DisplayName("NotificationServiceImpl Unit Tests")
    class ServiceTests {
        @Mock NotificationRepository notificationRepository;
        @Mock UserRepository userRepository;
        @Mock StoreRepository storeRepository;
        @Mock SimpMessagingTemplate messagingTemplate;
        @InjectMocks NotificationServiceImpl notificationService;

        @Test
        @DisplayName("sendNotification creates entity, saves, and pushes to user WebSocket topic")
        void sendNotification_success() {
            User user = User.builder().id(10L).username("customer1").build();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(100L);
                return n;
            });

            Notification result = notificationService.sendNotification(user, "Đơn mới", "Nội dung", "ORDER_CREATED", 50L);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Đơn mới");
            assertThat(result.getContent()).isEqualTo("Nội dung");
            assertThat(result.getType()).isEqualTo("ORDER_CREATED");
            assertThat(result.getRelatedOrderId()).isEqualTo(50L);
            assertThat(result.getIsRead()).isFalse();

            verify(messagingTemplate).convertAndSend(eq("/topic/user/10/notifications"), any(Notification.class));
        }

        @Test
        @DisplayName("sendNotification returns null when user is null")
        void sendNotification_nullUser() {
            Notification result = notificationService.sendNotification(null, "Test", "Content", "SYSTEM", null);
            assertThat(result).isNull();
            verifyNoInteractions(notificationRepository);
            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("sendNotification catches WebSocket push failure gracefully without failing transaction")
        void sendNotification_webSocketBypassed() {
            User user = User.builder().id(10L).username("customer1").build();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("WebSocket server offline"))
                    .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

            Notification result = notificationService.sendNotification(user, "Test", "Content", "SYSTEM", null);
            assertThat(result).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("sendNotificationToStoreManagers sends to owner and all store staff")
        void sendNotificationToStoreManagers_dispatchesToOwnerAndStaff() {
            User owner = User.builder().id(1L).username("owner").build();
            User staff1 = User.builder().id(2L).username("staff1").build();
            User staff2 = User.builder().id(3L).username("staff2").build();
            Store store = Store.builder().id(5L).name("Cửa hàng Q1").owner(owner).build();

            when(storeRepository.findById(5L)).thenReturn(Optional.of(store));
            when(userRepository.findByStoreId(5L)).thenReturn(List.of(owner, staff1, staff2));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

            notificationService.sendNotificationToStoreManagers(5L, "Đơn mới chi nhánh", "Chi tiết", "ORDER_CREATED", 99L);

            // Phải gửi cho owner (1 lần) và staff1, staff2 (mỗi người 1 lần, không bị trùng owner)
            verify(notificationRepository, times(3)).save(any(Notification.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/user/1/notifications"), any(Notification.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/user/2/notifications"), any(Notification.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/user/3/notifications"), any(Notification.class));
        }

        @Test
        @DisplayName("markAsRead marks read only when notification belongs to requesting user")
        void markAsRead_onlyOwnerCanMark() {
            User user = User.builder().id(10L).build();
            User otherUser = User.builder().id(99L).build();
            Notification notif = Notification.builder().id(1L).user(user).isRead(false).build();

            when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

            // Thử đánh dấu bởi user khác -> không thay đổi
            notificationService.markAsRead(1L, otherUser.getId());
            assertThat(notif.getIsRead()).isFalse();
            verify(notificationRepository, never()).save(any());

            // Đánh dấu bởi chính chủ -> thành công
            notificationService.markAsRead(1L, user.getId());
            assertThat(notif.getIsRead()).isTrue();
            verify(notificationRepository).save(notif);
        }

        @Test
        @DisplayName("markAllAsRead marks all unread notifications of user as read")
        void markAllAsRead_marksAll() {
            User user = User.builder().id(10L).build();
            Notification n1 = Notification.builder().id(1L).user(user).isRead(false).build();
            Notification n2 = Notification.builder().id(2L).user(user).isRead(true).build();
            Notification n3 = Notification.builder().id(3L).user(user).isRead(false).build();

            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(n1, n2, n3));

            notificationService.markAllAsRead(10L);

            assertThat(n1.getIsRead()).isTrue();
            assertThat(n2.getIsRead()).isTrue();
            assertThat(n3.getIsRead()).isTrue();
            verify(notificationRepository).saveAll(List.of(n1, n2, n3));
        }
    }

    @Nested
    @DisplayName("NotificationController Unit Tests")
    class ControllerTests {
        @Mock NotificationService notificationService;
        @Mock UserRepository userRepository;
        @Mock com.finalproject.service.StoreAccessService storeAccess;
        @InjectMocks NotificationController controller;

        @Test
        @DisplayName("viewAllNotifications redirects to /login when principal is null")
        void viewAll_unauthenticated() {
            Model model = new ConcurrentModel();
            String view = controller.viewAllNotifications(null, model);
            assertThat(view).isEqualTo("redirect:/login");
        }

        @Test
        @DisplayName("viewAllNotifications populates model and returns customer/notifications view")
        void viewAll_authenticated() {
            User user = User.builder().id(10L).username("customer1").build();
            Notification n1 = Notification.builder().id(1L).title("T1").isRead(false).build();
            when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
            when(notificationService.getUserNotifications(10L)).thenReturn(List.of(n1));
            when(notificationService.getUnreadCount(10L)).thenReturn(1L);

            Model model = new ConcurrentModel();
            String view = controller.viewAllNotifications(() -> "customer1", model);

            assertThat(view).isEqualTo("customer/notifications");
            assertThat(model.getAttribute("notifications")).isEqualTo(List.of(n1));
            assertThat(model.getAttribute("unreadCount")).isEqualTo(1L);
            assertThat(model.getAttribute("user")).isEqualTo(user);
        }

        @Test
        @DisplayName("viewAllNotifications returns manager/notifications view for manager role")
        void viewAll_managerRole() {
            User manager = User.builder().id(20L).username("manager1").role(com.finalproject.enums.Role.MANAGER).build();
            Store store = Store.builder().id(1L).name("Tiệm Hoa Quận 1").build();
            when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
            when(notificationService.getUserNotifications(20L)).thenReturn(List.of());
            when(notificationService.getUnreadCount(20L)).thenReturn(0L);
            when(storeAccess.selectedStore()).thenReturn(store);

            Model model = new ConcurrentModel();
            String view = controller.viewAllNotifications(() -> "manager1", model);

            assertThat(view).isEqualTo("manager/notifications");
            assertThat(model.getAttribute("managerStore")).isEqualTo(store);
        }

        @Test
        @DisplayName("getUnreadCount returns correct json")
        void getUnreadCount_api() {
            User user = User.builder().id(10L).username("customer1").build();
            when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
            when(notificationService.getUnreadCount(10L)).thenReturn(3L);

            ResponseEntity<Map<String, Object>> response = controller.getUnreadCount(() -> "customer1");
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("unreadCount")).isEqualTo(3L);
        }

        @Test
        @DisplayName("getLatestNotifications returns top 6 mapped records")
        void getLatestNotifications_api() {
            User user = User.builder().id(10L).username("customer1").build();
            Notification n1 = Notification.builder()
                    .id(1L).title("T1").content("C1").type("ORDER_CREATED")
                    .isRead(false).relatedOrderId(88L).createdAt(LocalDateTime.of(2026, 10, 4, 12, 0))
                    .build();

            when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
            when(notificationService.getUserNotifications(10L)).thenReturn(List.of(n1));

            ResponseEntity<List<Map<String, Object>>> response = controller.getLatestNotifications(() -> "customer1");
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            Map<String, Object> map = response.getBody().get(0);
            assertThat(map.get("id")).isEqualTo(1L);
            assertThat(map.get("title")).isEqualTo("T1");
            assertThat(map.get("relatedOrderId")).isEqualTo(88L);
            assertThat(map.get("type")).isEqualTo("ORDER_CREATED");
        }

        @Test
        @DisplayName("markAsRead marks read for authenticated principal")
        void markAsRead_api() {
            User user = User.builder().id(10L).username("customer1").build();
            when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));

            ResponseEntity<?> res = controller.markAsRead(5L, () -> "customer1");
            assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
            verify(notificationService).markAsRead(5L, 10L);
        }

        @Test
        @DisplayName("markAllAsRead marks all read for authenticated principal")
        void markAllAsRead_api() {
            User user = User.builder().id(10L).username("customer1").build();
            when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));

            ResponseEntity<?> res = controller.markAllAsRead(() -> "customer1");
            assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
            verify(notificationService).markAllAsRead(10L);
        }
    }
}
