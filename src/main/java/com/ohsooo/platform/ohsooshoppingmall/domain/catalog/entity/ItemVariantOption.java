package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(     // TODO: DB에 유니크 제약 추가
        name = "item_variant_option",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"item_variant_id", "option_id"}
                )
        }
)
public class ItemVariantOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="item_variant_option_id")
    private Long itemVariantOptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private Option option;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_variant_id", nullable = false)
    private ItemVariant itemVariant;

    public ItemVariantOption(Option option, ItemVariant itemVariant) {
        this.option = option;
        this.itemVariant = itemVariant;
    }
}
