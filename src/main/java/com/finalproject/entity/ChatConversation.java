package com.finalproject.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name="chat_conversations", uniqueConstraints=@UniqueConstraint(columnNames="conversation_key"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatConversation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="conversation_key",nullable=false,length=100) private String conversationKey;
 @Column(nullable=false,length=16) private String kind;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id",nullable=false) private User customer;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="store_id") private Store store;
 @org.hibernate.annotations.Nationalized @Column(length=2000) private String lastMessage;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false) private LocalDateTime updatedAt;
}
