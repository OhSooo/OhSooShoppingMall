package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentEventType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 결제 관련 이벤트 로그 엔티티.
 *
 * 웹훅 수신, 클라이언트 confirm 요청 등
 * "결제와 관련된 사실"을 원문(JSON) 그대로 저장하기 위한 테이블.
 *
 * 목적:
 * - 디버깅
 * - 분쟁 대응
 * - PG 통신 추적
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payment_events")
public class PaymentEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_event_id", nullable = false)
  private Long paymentEventId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payment_id", nullable = false)
  private Payment payment;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  private PaymentEventType eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
  private String payloadJson;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public static PaymentEvent of(Payment payment, PaymentEventType eventType, String payloadJson) {
    PaymentEvent e = new PaymentEvent();
    e.payment = payment;
    e.eventType = eventType;
    e.payloadJson = payloadJson;
    e.createdAt = OffsetDateTime.now();
    return e;
  }
}
