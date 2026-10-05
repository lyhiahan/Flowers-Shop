package com.finalproject.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name="chat_messages", uniqueConstraints=@UniqueConstraint(columnNames={"conversation_id","sender_id","client_message_id"}),
 indexes=@Index(name="idx_chat_messages_conversation_id",columnList="conversation_id,id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="conversation_id",nullable=false) private ChatConversation conversation;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sender_id",nullable=false) private User sender;
 @Column(name="client_message_id",nullable=false,length=36) private String clientMessageId;
 @org.hibernate.annotations.Nationalized @Column(nullable=false,length=2000) private String content;
 @Column(length=500) private String imageUrl;
 @Column(length=255) private String imagePublicId;
 @Column(nullable=false) private LocalDateTime createdAt;
}
