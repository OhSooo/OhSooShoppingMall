package com.ohsooo.platform.ohsooshoppingmall.domain.order.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상품 엔티티(주문 아이템 단위).
 * - 구매 당시 가격(priceAtPurchase)을 저장해서, 이후 가격이 바뀌어도 주문 내역이 흔들리지 않게 한다.
 * - 상태는 배송/취소/환불을 item 단위로 관리한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "order_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_order_item", columnNames = {"order_id", "item_variant_id"})
    }
)
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_item_id")
  private Long orderItemId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_variant_id", nullable = false)
  private ItemVariant itemVariant;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private OrderItemStatus status;

  @Column(name = "price_at_purchase", nullable = false, precision = 19, scale = 2)
  private BigDecimal priceAtPurchase;

  @Column(name = "product_name", length = 255)
  private String productName;

  @Column(name = "option_summary", length = 255)
  private String optionSummary;

  private OrderItem(ItemVariant itemVariant, int quantity, BigDecimal priceAtPurchase,
      String productName, String optionSummary) {
    this.itemVariant = itemVariant;
    this.quantity = Math.max(quantity, 1);
    this.priceAtPurchase = (priceAtPurchase == null) ? BigDecimal.ZERO : priceAtPurchase;
    this.status = OrderItemStatus.ORDERED;
    this.productName = productName;
    this.optionSummary = (optionSummary == null) ? "" : optionSummary;
  }

  public static OrderItem of(ItemVariant itemVariant, int quantity, BigDecimal priceAtPurchase,
      String productName, String optionSummary) {
    return new OrderItem(itemVariant, quantity, priceAtPurchase, productName, optionSummary);
  }

  void attachTo(Order order) {
    this.order = order;
  }

  public void changeStatus(OrderItemStatus newStatus) {
    if (newStatus == null) return;
    this.status = newStatus;
  }

  public boolean isSameOrder(Long orderId) {
    return this.order != null
        && this.order.getOrderId() != null
        && this.order.getOrderId().equals(orderId);
  }
}
