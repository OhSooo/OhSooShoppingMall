package com.ohsooo.platform.ohsooshoppingmall.domain.order.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문상품 상태 변경 이력 엔티티.
 * - OrderItem의 status 변경을 누가/언제/어떤 상태에서 어떤 상태로 바꿨는지 기록한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_item_histories")
public class OrderItemHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_item_history_id")
  private Long orderItemHistoryId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_item_id", nullable = false)
  private OrderItem orderItem;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", nullable = false, length = 30)
  private OrderItemStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 30)
  private OrderItemStatus newStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "changed_by", nullable = false, length = 20)
  private OrderChangedBy changedBy;

  @Column(name = "changed_at", nullable = false)
  private OffsetDateTime changedAt;

  private OrderItemHistory(
      OrderItem orderItem,
      OrderItemStatus previousStatus,
      OrderItemStatus newStatus,
      OrderChangedBy changedBy,
      OffsetDateTime changedAt
  ) {
    this.orderItem = orderItem;
    this.previousStatus = previousStatus;
    this.newStatus = newStatus;
    this.changedBy = changedBy;
    this.changedAt = (changedAt == null) ? OffsetDateTime.now() : changedAt;
  }

  /** 기본: changedAt은 now() */
  public static OrderItemHistory create(
      OrderItem orderItem,
      OrderItemStatus previousStatus,
      OrderItemStatus newStatus,
      OrderChangedBy changedBy
  ) {
    return new OrderItemHistory(orderItem, previousStatus, newStatus, changedBy, OffsetDateTime.now());
  }

  /** 오버로드: changedAt을 외부에서 주입하고 싶을 때 */
  public static OrderItemHistory create(
      OrderItem orderItem,
      OrderItemStatus previousStatus,
      OrderItemStatus newStatus,
      OrderChangedBy changedBy,
      OffsetDateTime changedAt
  ) {
    return new OrderItemHistory(orderItem, previousStatus, newStatus, changedBy, changedAt);
  }
}
