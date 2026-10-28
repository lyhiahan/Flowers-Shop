package com.finalproject.entity;

import com.finalproject.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "promotions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer discountPercentage;

    private LocalDate startDate;
    private LocalDate endDate;

    // NULL là áp dụng toàn chuỗi (Admin), có giá trị là Sale nội bộ (Manager)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private OrderType applyType; // BUY hoặc null nếu áp dụng chung cho bán hoa
}
