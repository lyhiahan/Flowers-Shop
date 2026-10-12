package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "jwt_refresh_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JwtRefreshToken {
    @Id @Column(length = 64)
    private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private JwtSession session;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean consumed;
}
