package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 품목 엔티티.
 *
 * 환불(Refund) 하나에 포함된 개별 주문상품(orderItem) 단위의 환불 정보.
 *
 * - 어떤 주문상품을
 * - 몇 개(quantity)
 * - 얼마(amount) 환불했는지 기록
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refund_item")
public class RefundItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "refunditem_id", nullable = false)
  private Long refundItemId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "refund_id", nullable = false)
  private Refund refund;

  @Column(name = "order_item_id", nullable = false)
  private Long orderItemId;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  public static RefundItem of(Refund refund, Long orderItemId, BigDecimal amount, Integer quantity) {
    RefundItem ri = new RefundItem();
    ri.refund = refund;
    ri.orderItemId = orderItemId;
    ri.amount = (amount == null) ? BigDecimal.ZERO : amount;
    ri.quantity = quantity;
    return ri;
  }
}
