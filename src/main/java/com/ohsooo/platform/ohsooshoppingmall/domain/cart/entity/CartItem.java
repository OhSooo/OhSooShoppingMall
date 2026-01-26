package com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_cart_item", columnNames = {"cart_id", "item_variant_id"})
    }
)
public class CartItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cart_item_id")
  private Long cartItemId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cart_id", nullable = false)
  private Cart cart;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_variant_id", nullable = false)
  private ItemVariant itemVariant;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  private CartItem(ItemVariant itemVariant, int quantity) {
    this.itemVariant = itemVariant;
    this.quantity = Math.max(quantity, 1);
  }

  public static CartItem of(ItemVariant itemVariant, int quantity) {
    return new CartItem(itemVariant, quantity);
  }

  void attachTo(Cart cart) {
    this.cart = cart;
  }

  void detach() {
    this.cart = null;
  }

  public boolean isSameVariant(Long itemVariantId) {
    return this.itemVariant != null
        && this.itemVariant.getItemVariantId() != null
        && this.itemVariant.getItemVariantId().equals(itemVariantId);
  }

  public void changeQuantity(int quantity) {
    this.quantity = Math.max(quantity, 1);
  }

  public void increaseQuantity(int amount) {
    if (amount <= 0) return;
    this.quantity += amount;
  }
}
