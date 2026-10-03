package com.finalproject.service;

import com.finalproject.entity.Notification;
import com.finalproject.entity.User;
import java.util.List;

public interface NotificationService {
    Notification sendNotification(User user, String title, String content, String type, Long relatedOrderId);
    void sendNotificationToStoreManagers(Long storeId, String title, String content, String type, Long relatedOrderId);
    List<Notification> getUserNotifications(Long userId);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
}
