package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 200)
    private String title;

    @org.hibernate.annotations.Nationalized
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NVARCHAR)
    @Column(nullable = false, length = org.hibernate.Length.LONG32)
    private String content;

    @Column(length = 50)
    private String type; // ORDER_UPDATE, OVERDUE_REMINDER, PROMOTION

    private Long relatedOrderId;

    @Builder.Default
    private Boolean isRead = false;

    private LocalDateTime createdAt;

    @Transient
    private String targetUrl;
}
