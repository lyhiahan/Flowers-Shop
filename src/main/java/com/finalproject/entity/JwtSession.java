package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "jwt_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JwtSession {
    @Id @Column(length = 36)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 64)
    private String credentialHash;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean revoked;
}
