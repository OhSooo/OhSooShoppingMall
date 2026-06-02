package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "item_variant_options")
public class ItemVariantOption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_variant_option_id")
  private Long itemVariantOptionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_variant_id", nullable = false)
  private ItemVariant itemVariant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "option_id", nullable = false)
  private Option option;

  public ItemVariantOption(ItemVariant itemVariant, Option option) {
    this.itemVariant = itemVariant;
    this.option = option;
  }
}
