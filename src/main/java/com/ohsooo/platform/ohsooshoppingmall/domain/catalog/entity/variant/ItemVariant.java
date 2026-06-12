package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "item_variants")
public class ItemVariant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_variant_id")
  private Long itemVariantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

  @Column(nullable = false, length = 100, unique = true)
  private String sku;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItemVariantStatus status;

  @OneToMany(
      mappedBy = "itemVariant",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private Set<ItemVariantOption> itemVariantOptions = new HashSet<>();

  public ItemVariant(Item item, String sku, BigDecimal price) {
    this.item = item;
    this.sku = sku;
    this.price = price;
    this.status = ItemVariantStatus.ACTIVE;
  }

  // Inventory 수량 변경 후 상태를 동기화한다. DISABLED 상태는 재고와 무관하게 유지된다.
  public void syncStatus(int currentQuantity) {
    if (this.status == ItemVariantStatus.DISABLED) return;
    this.status = (currentQuantity > 0) ? ItemVariantStatus.ACTIVE : ItemVariantStatus.OUT_OF_STOCK;
  }

  public void disable() {
    this.status = ItemVariantStatus.DISABLED;
  }

  // DISABLED 해제 시 현재 재고를 기준으로 상태를 복원한다.
  public void enable(int currentQuantity) {
    this.status = (currentQuantity > 0) ? ItemVariantStatus.ACTIVE : ItemVariantStatus.OUT_OF_STOCK;
  }
}
