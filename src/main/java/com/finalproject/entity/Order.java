package com.finalproject.entity;

import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType orderType;

    // Chi nhánh xuất đồ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_store_id", nullable = false)
    private Store pickupStore;

    // Chi nhánh nhận trả đồ (hỗ trợ trả chéo kho)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_store_id")
    private Store returnStore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    private BigDecimal totalAmount;
    private BigDecimal totalDeposit;
    private BigDecimal fineAmount; // Tiền phạt quá hạn/hư hỏng

    private LocalDateTime createdAt;

    @Version
    private Long version; // Khóa lạc quan chống duyệt trùng

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;
}