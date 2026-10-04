package com.finalproject.service.impl;

import com.finalproject.entity.Notification;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.NotificationRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Notification sendNotification(User user, String title, String content, String type, Long relatedOrderId) {
        if (user == null) return null;
        Notification notif = Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .type(type != null ? type : "SYSTEM")
                .relatedOrderId(relatedOrderId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(notif);

        // Bắn WebSocket thời gian thực nếu người dùng đang online
        try {
            messagingTemplate.convertAndSend("/topic/user/" + user.getId() + "/notifications", saved);
        } catch (Exception ex) {
            log.debug("WebSocket push notification bypassed: {}", ex.getMessage());
        }
        return saved;
    }

    @Override
    @Transactional
    public void sendNotificationToStoreManagers(Long storeId, String title, String content, String type, Long relatedOrderId) {
        if (storeId == null) return;
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) return;

        // Gửi cho chủ cửa hàng
        if (store.getOwner() != null) {
            sendNotification(store.getOwner(), title, content, type, relatedOrderId);
        }

        // Gửi cho tất cả nhân viên / quản lý trực thuộc chi nhánh
        List<User> storeStaff = userRepository.findByStoreId(storeId);
        for (User u : storeStaff) {
            if (store.getOwner() == null || !u.getId().equals(store.getOwner().getId())) {
                sendNotification(u, title, content, type, relatedOrderId);
            }
        }
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser() != null && n.getUser().getId().equals(userId)) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Notification n : list) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }
}
