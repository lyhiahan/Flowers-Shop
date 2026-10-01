package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Refresh token lưu trong DB để có thể revoke (thu hồi) bất kỳ lúc nào.
 * Bảng được tạo qua repair-schema.sql.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Giá trị token (UUID ngẫu nhiên, lưu hash trong production nếu cần) */
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    /** Thời điểm hết hạn */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** true khi đã bị thu hồi (logout hoặc rotate) */
    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
