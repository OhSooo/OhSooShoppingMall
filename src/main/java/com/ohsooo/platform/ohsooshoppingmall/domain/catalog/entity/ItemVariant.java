package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.ItemVariantErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "item_variants",
        uniqueConstraints = {
                @UniqueConstraint(name= "uk_item_variant_sku", columnNames = "sku")
        })
public class ItemVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_variant_id")
    private Long itemVariantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;           // TODO: db 수정 필요

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private ItemVariantStatus status;

    public ItemVariant(Item item, String sku, BigDecimal price, int quantity, ItemVariantStatus status) {
        if (quantity < 0) {
            throw new BusinessException(ItemVariantErrorCode.ITEM_VARIANT_INVALID_QUANTITY);
        }

        this.item = item;
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
    }

    @PrePersist
    protected void prePersist() {
        if (status == null){
            this.status = ItemVariantStatus.AVAILABLE;
        }
    }
}
