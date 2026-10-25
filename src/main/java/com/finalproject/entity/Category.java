package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @org.hibernate.annotations.Nationalized
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NVARCHAR)
    @Column(length = org.hibernate.Length.LONG32)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<Category> subCategories = new java.util.ArrayList<>();

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
}
