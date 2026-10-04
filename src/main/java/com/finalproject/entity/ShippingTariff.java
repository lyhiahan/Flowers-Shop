package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_tariffs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingTariff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store; // Điểm gửi: Chi nhánh thực hiện đơn (null = Áp dụng chung toàn hệ thống)

    @org.hibernate.annotations.Nationalized
    @Column(name = "destination_area", nullable = false, length = 150)
    private String destinationArea; // Khu vực nhận (ví dụ: "Khu vực gần chi nhánh (< 5km)", "Khu vực xa hơn (5 - 15km)", "Khu vực ngoại thành (> 15km)")

    @org.hibernate.annotations.Nationalized
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName; // Loại dịch vụ: "Tiêu chuẩn", "Hỏa tốc", "Siêu tốc hẹn giờ"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery; // Đơn vị giao hàng phụ trách (ví dụ GrabExpress, Ahamove...)

    @Column(name = "base_fee", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal baseFee = BigDecimal.ZERO; // Mức phí cơ bản (25.000đ, 40.000đ, 50.000đ...)

    @Column(name = "bulky_surcharge", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal bulkySurcharge = BigDecimal.ZERO; // Phụ phí kệ hoa lớn / cần xe riêng (20.000đ, 30.000đ...)

    @org.hibernate.annotations.Nationalized
    @Column(name = "estimated_time", length = 100)
    private String estimatedTime; // Thời gian dự kiến: "4 - 6 giờ", "Trong 2 giờ", "60 phút"

    @org.hibernate.annotations.Nationalized
    @Column(length = 500)
    private String note; // Ghi chú cấu hình

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (baseFee == null) baseFee = BigDecimal.ZERO;
        if (bulkySurcharge == null) bulkySurcharge = BigDecimal.ZERO;
        if (isActive == null) isActive = true;
        if (isDeleted == null) isDeleted = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
