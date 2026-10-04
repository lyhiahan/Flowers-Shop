package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "user_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StoreMembership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id", nullable = false) private Store store;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 16) private String accessRole;
    @Builder.Default @Column(nullable = false) private Boolean active = true;
    @Builder.Default private LocalDateTime joinedAt = LocalDateTime.now();
}
