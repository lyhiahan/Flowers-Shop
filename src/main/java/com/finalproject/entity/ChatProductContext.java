package com.finalproject.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
/** Snapshot of the product being asked about, independent of later product edits. */
@Entity @Table(name="chat_product_contexts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatProductContext {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="message_id",nullable=false,unique=true) private ChatMessage message;
 @Column(name="product_id",nullable=false) private Long productId;
 @org.hibernate.annotations.Nationalized @Column(name="product_name",nullable=false,length=200) private String productName;
 @Column(name="product_image_url",length=500) private String productImageUrl;
 @Column(name="product_price",nullable=false,precision=38,scale=2) private BigDecimal productPrice;
}
