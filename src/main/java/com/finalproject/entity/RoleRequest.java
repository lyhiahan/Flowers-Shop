package com.finalproject.entity;

import com.finalproject.enums.Role;
import com.finalproject.enums.RoleRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = RoleConverter.class)
    @Column(name = "target_role", nullable = false, length = 30)
    private Role targetRole;

    @Column(name = "target_role", nullable = false, length = 30, insertable = false, updatable = false)
    private String storedTargetRole;

    public boolean isRetiredRoleRequest() {
        return "SHIPPER".equals(storedTargetRole);
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RoleRequestStatus status = RoleRequestStatus.PENDING;

    @Column(length = 20)
    private String phone;

    @org.hibernate.annotations.Nationalized
    @Column(name = "work_area", length = 255)
    private String workArea; // Retained for historical requests only.

    @org.hibernate.annotations.Nationalized
    @Column(name = "vehicle_type", length = 100)
    private String vehicleType; // Retained for historical requests only.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_store_id")
    private Store targetStore;

    @org.hibernate.annotations.Nationalized
    @Column(name = "shop_name", length = 255)
    private String shopName;

    @org.hibernate.annotations.Nationalized
    @Column(name = "shop_address", length = 500)
    private String shopAddress;

    @org.hibernate.annotations.Nationalized
    @Column(name = "contact_info", length = 255)
    private String contactInfo;

    @org.hibernate.annotations.Nationalized
    @Column(name = "experience_note", length = 1000)
    private String experienceNote;

    @org.hibernate.annotations.Nationalized
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_store_id")
    private Store assignedStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getTargetRoleDisplayName() {
        if (isRetiredRoleRequest()) return "Vai trò đã ngừng sử dụng";
        if (targetRole == null) return "Chưa xác định";
        return switch (targetRole) {
            case MANAGER -> "Quản lý chi nhánh";
            case VENDOR -> "Người bán (Đối tác)";
            case STAFF -> "Nhân viên chi nhánh";
            default -> targetRole.name();
        };
    }
}
