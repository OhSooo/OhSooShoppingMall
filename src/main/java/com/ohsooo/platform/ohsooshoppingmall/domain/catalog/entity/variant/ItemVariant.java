package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
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

  @Column(nullable = false)
  private int price;

  @Column(nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItemVariantStatus status;

  @OneToMany(
      mappedBy = "itemVariant",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private List<ItemVariantOption> itemVariantOptions = new ArrayList<>();

  public ItemVariant(Item item, String sku, int price, int quantity) {
    this.item = item;
    this.sku = sku;
    this.price = price;
    this.quantity = quantity;
    this.status = (quantity > 0) ? ItemVariantStatus.ACTIVE : ItemVariantStatus.OUT_OF_STOCK;
  }

  // (1) 스토어 관리자가 재고 입고 처리 시 (2) 취소/환불 등으로 재고가 복원 될 때 호출됨.
  public void increaseQuantity(int amount) {
    if (amount <= 0) return;
    this.quantity += amount;
    if (this.quantity > 0 && this.status == ItemVariantStatus.OUT_OF_STOCK) {
      this.status = ItemVariantStatus.ACTIVE;
    }
  }

  public void decreaseQuantity(int amount) {
    if (amount <= 0) return;
    int next = this.quantity - amount;
    if (next < 0) {
      throw new IllegalArgumentException("Insufficient stock");
    }
    this.quantity = next;
    if (this.quantity == 0) {
      this.status = ItemVariantStatus.OUT_OF_STOCK;
    }
  }

  // 판매 중지 처리
  public void disable() {
    this.status = ItemVariantStatus.DISABLED;
  }

  // 판매 재개 처리
  public void enable() {
    if (this.quantity > 0) this.status = ItemVariantStatus.ACTIVE;
    else this.status = ItemVariantStatus.OUT_OF_STOCK;
  }
}
