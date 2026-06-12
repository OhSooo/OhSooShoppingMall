package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inventories")
public class Inventory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "inventory_id")
  private Long inventoryId;

  @Column(name = "item_variant_id", nullable = false, unique = true)
  private Long itemVariantId;

  @Column(nullable = false)
  private int quantity;

  public static Inventory of(Long itemVariantId, int initialQuantity) {
    Inventory inventory = new Inventory();
    inventory.itemVariantId = itemVariantId;
    inventory.quantity = initialQuantity;
    return inventory;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
