package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int depth;

    @Column(name="display_order", nullable = false)
    private int displayOrder;

    @Column(name="is_active")
    private boolean isActive;

    public Category(Category parent, String name, int depth, int displayOrder) {
        this.parent = parent;
        this.name = name;
        this.depth = depth;
        this.displayOrder = displayOrder;
    }

    @PrePersist
    protected void onCreate() {
        this.isActive = true;
    }
}
