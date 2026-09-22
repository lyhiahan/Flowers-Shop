package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store_inventories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"store_id", "product_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Version
    private Long version; // Chống race condition
}