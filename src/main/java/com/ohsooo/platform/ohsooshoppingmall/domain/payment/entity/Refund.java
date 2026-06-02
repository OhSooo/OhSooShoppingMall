package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.RefundStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 엔티티.
 *
 * 하나의 결제(Payment)에 대해 발생한 "환불 요청"을 표현한다.
 *
 * - 부분 환불 / 전액 환불 모두 이 엔티티로 관리
 * - 실제 환불 품목 단위는 RefundItem에서 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refunds")
public class Refund {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "refund_id", nullable = false)
  private Long refundId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payment_id", nullable = false)
  private Payment payment;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private RefundStatus status;

  @Column(name = "reason")
  private String reason;

  @Column(name = "refunded_at")
  private OffsetDateTime refundedAt;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public static Refund requested(Payment payment, BigDecimal amount, String reason) {
    Refund r = new Refund();
    r.payment = payment;
    r.amount = (amount == null) ? BigDecimal.ZERO : amount;
    r.reason = reason;
    r.status = RefundStatus.REQUESTED;
    r.createdAt = OffsetDateTime.now();
    return r;
  }

  public void markSucceeded() {
    this.status = RefundStatus.SUCCEEDED;
    this.refundedAt = OffsetDateTime.now();
  }

  public void markFailed() {
    this.status = RefundStatus.FAILED;
  }
}
