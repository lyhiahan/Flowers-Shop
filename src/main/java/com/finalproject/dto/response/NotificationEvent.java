package com.finalproject.dto.response;

import com.finalproject.entity.Notification;
import java.time.LocalDateTime;

/** Public realtime payload, deliberately independent of JPA account relations. */
public record NotificationEvent(Long id, String title, String content, String type,
                                Long relatedOrderId, Boolean isRead, LocalDateTime createdAt,
                                String targetUrl) {

    public NotificationEvent(Long id, String title, String content, String type,
                             Long relatedOrderId, Boolean isRead, LocalDateTime createdAt) {
        this(id, title, content, type, relatedOrderId, isRead, createdAt, null);
    }

    public static NotificationEvent from(Notification notification, String targetUrl) {
        if (notification == null) return null;
        return new NotificationEvent(notification.getId(), notification.getTitle(), notification.getContent(),
                notification.getType(), notification.getRelatedOrderId(), notification.getIsRead(), notification.getCreatedAt(),
                targetUrl);
    }

    public static NotificationEvent from(Notification notification) {
        if (notification == null) return null;
        return from(notification, notification.getTargetUrl());
    }
}
