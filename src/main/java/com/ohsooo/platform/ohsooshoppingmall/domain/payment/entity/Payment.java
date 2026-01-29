package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentMethod;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentProvider;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentStatus;
import com.ohsooo.platform.ohsooshoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제(Payment) 엔티티.
 *
 * "주문(order)"에 대해 발생한 하나의 결제 시도를 표현한다.
 *
 * 사용 시점:
 * - 클라이언트가 결제 버튼을 누르면 READY 상태로 생성됨
 * - 결제 확정(confirm) 시 PG 승인 결과에 따라 CAPTURED / FAILED 로 변경
 * - 취소 / 환불 시 CANCELED / REFUNDED 로 변경
 *
 * 결제 상태 머신의 중심 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id"),
        @Index(name = "idx_payments_pg_payment_key", columnList = "pg_payment_key"),
        @Index(name = "idx_payments_pg_transaction_id", columnList = "pg_transaction_id")
    })
public class Payment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_id", nullable = false)
  private Long paymentId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private PaymentStatus status;

  @Column(name = "amount", nullable = false)
  private Integer amount;

  @Column(name = "currency", nullable = false, length = 10)
  private String currency = "KRW";

  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false, length = 30)
  private PaymentMethod method;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 30)
  private PaymentProvider provider;

  @Column(name = "pg_payment_key", length = 255, unique = true)
  private String pgPaymentKey;

  @Column(name = "pg_transaction_id", length = 255, unique = true)
  private String pgTransactionId;

  @Column(name = "requested_at", nullable = false)
  private OffsetDateTime requestedAt;

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

  @Column(name = "failed_at")
  private OffsetDateTime failedAt;

  @Column(name = "fail_reason", length = 255)
  private String failReason;

  /**
   * 결제 시작 시 READY 상태의 Payment를 생성한다.
   */
  public static Payment ready(
      Long orderId,
      Integer amount,
      String currency,
      PaymentMethod method,
      PaymentProvider provider
  ) {
    Payment p = new Payment();
    p.orderId = orderId;
    p.amount = amount;
    p.currency = (currency == null || currency.isBlank()) ? "KRW" : currency;
    p.method = method;
    p.provider = provider;
    p.status = PaymentStatus.READY;
    p.requestedAt = OffsetDateTime.now();
    return p;
  }

  /**
   * 결제 승인(또는 승인+캡처) 성공 시 상태 변경.
   */
  public void markCaptured(String pgPaymentKey, String pgTransactionId, OffsetDateTime approvedAt) {
    if (this.status != PaymentStatus.READY && this.status != PaymentStatus.AUTHORIZED) {
      throw new IllegalStateException("Payment cannot be captured from status: " + this.status);
    }
    this.status = PaymentStatus.CAPTURED;
    this.pgPaymentKey = pgPaymentKey;
    this.pgTransactionId = pgTransactionId;
    this.approvedAt = approvedAt != null ? approvedAt : OffsetDateTime.now();
    this.failedAt = null;
    this.failReason = null;
  }

  /**
   * 결제 실패 처리.
   */
  public void markFailed(String reason) {
    if (this.status == PaymentStatus.CAPTURED
        || this.status == PaymentStatus.CANCELED
        || this.status == PaymentStatus.REFUNDED) {
      throw new IllegalStateException("Payment cannot be failed from status: " + this.status);
    }
    this.status = PaymentStatus.FAILED;
    this.failedAt = OffsetDateTime.now();
    this.failReason = reason;
  }
}
