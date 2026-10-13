package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="chat_access_audits", indexes={
    @Index(name="idx_chat_audit_admin_time",columnList="admin_id,accessed_at"),
    @Index(name="idx_chat_audit_conversation_time",columnList="conversation_id,accessed_at")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatAccessAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="admin_id",nullable=false) private User administrator;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="conversation_id",nullable=false) private ChatConversation conversation;
    @Column(name="accessed_at",nullable=false) private LocalDateTime accessedAt;
    @Column(name="before_message_id") private Long beforeMessageId;
}
