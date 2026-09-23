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

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    private String avatarUrl;

    @Column(length = 500)
    private String cover;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Builder.Default
    private Boolean isActive = false;

    private LocalDate createdDate;

    // Chi nhánh làm việc (NULL đối với Customer và Admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;
}