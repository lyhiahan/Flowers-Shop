package com.finalproject.entity;

import com.finalproject.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @org.hibernate.annotations.Nationalized
    @Column(length = 255)
    private String address;

    private String avatarUrl;

    /** public_id trên Cloudinary của avatar – dùng để xóa ảnh cũ */
    @Column(name = "avatar_public_id", length = 255)
    private String avatarPublicId;

    @Column(length = 500)
    private String cover;

    /** public_id trên Cloudinary của cover – dùng để xóa ảnh cũ */
    @Column(name = "cover_public_id", length = 255)
    private String coverPublicId;

    @Builder.Default
    private Integer point = 0;

    @Column(name = "e_wallet", precision = 18, scale = 2)
    @Builder.Default
    private java.math.BigDecimal eWallet = java.math.BigDecimal.ZERO;

    @Column(name = "is_email_active", nullable = false)
    @Builder.Default
    private Boolean isEmailActive = true;

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "otp_expiry")
    private java.time.LocalDateTime otpExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_level_id")
    private UserLevel userLevel;

    @Convert(converter = RoleConverter.class)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "role", nullable = false, length = 20, insertable = false, updatable = false)
    private String storedRole;

    @Column(name = "retired_delivery_account")
    @Builder.Default
    private Boolean retiredDeliveryAccount = false;

    @Column(name = "account_disabled_by_admin")
    @Builder.Default
    private Boolean accountDisabledByAdmin = false;

    public boolean isRetiredDeliveryAccount() {
        return Boolean.TRUE.equals(retiredDeliveryAccount) || "SHIPPER".equals(storedRole);
    }

    @PostLoad
    private void disableRetiredDeliveryAccount() {
        if ("SHIPPER".equals(storedRole)) retiredDeliveryAccount = true;
        if (isRetiredDeliveryAccount() || Boolean.TRUE.equals(accountDisabledByAdmin)) isActive = false;
    }

    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Convert(converter = RoleConverter.class)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private java.util.Set<Role> roles = new java.util.HashSet<>();

    @Builder.Default
    private Boolean isActive = false;

    private LocalDate createdDate;
    private java.time.LocalDateTime createdAt;
    @Builder.Default private Boolean creationTimeKnown = false;

    @PrePersist
    private void initializeCreationTime() {
        if (createdAt == null) {
            if (createdDate != null) createdAt = createdDate.atStartOfDay();
            else {
                createdAt = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Bangkok"));
                createdDate = createdAt.toLocalDate();
                creationTimeKnown = true;
            }
        }
    }

    // Chi nhánh làm việc (NULL đối với Customer và Admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    public boolean hasRole(Role checkRole) {
        if (this.role == checkRole) return true;
        return roles != null && roles.contains(checkRole);
    }

    public void addRole(Role newRole) {
        if (this.roles == null) this.roles = new java.util.HashSet<>();
        this.roles.add(newRole);
        this.roles.add(Role.CUSTOMER); // Luôn giữ quyền khách hàng
    }

    public void removeRole(Role targetRole) {
        if (this.roles != null) {
            this.roles.remove(targetRole);
            this.roles.add(Role.CUSTOMER); // Không bao giờ mất quyền khách hàng
        }
        if (this.role == targetRole) {
            if (this.roles != null && !this.roles.isEmpty()) {
                this.role = this.roles.stream()
                        .filter(r -> r != Role.CUSTOMER)
                        .findFirst()
                        .orElse(Role.CUSTOMER);
            } else {
                this.role = Role.CUSTOMER;
            }
        }
    }
}
