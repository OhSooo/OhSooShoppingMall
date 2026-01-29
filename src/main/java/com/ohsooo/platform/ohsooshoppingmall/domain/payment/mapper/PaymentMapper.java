package com.ohsooo.platform.ohsooshoppingmall.domain.payment.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.PaymentCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentConfirmResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentCreateResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.PaymentEvent;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentEventType;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

  /** 결제 준비(READY) 엔티티 생성 */
  public Payment toReadyPaymentEntity(PaymentCreateRequestDto request) {
    if (request == null) return null;

    return Payment.ready(
        request.getOrderId(),
        request.getAmount(),
        request.getCurrency(),
        request.getMethod(),
        request.getProvider()
    );
  }

  /** 결제 준비 응답 DTO (액션 결과) */
  public PaymentCreateResponseDto toCreateResponseDto(Payment payment, PaymentCreateRequestDto request) {
    if (payment == null) return null;

    return new PaymentCreateResponseDto(
        payment.getPaymentId(),
        payment.getOrderId(),
        payment.getStatus().name(),
        payment.getAmount(),
        payment.getCurrency(),
        payment.getMethod(),
        payment.getProvider(),
        payment.getRequestedAt(),
        request != null ? request.getOrderName() : null,
        request != null ? request.getCustomerName() : null
    );
  }

  /** 결제 확정 응답 DTO (액션 결과) */
  public PaymentConfirmResponseDto toConfirmResponseDto(Payment payment) {
    if (payment == null) return null;

    return new PaymentConfirmResponseDto(
        payment.getPaymentId(),
        payment.getOrderId(),
        payment.getStatus().name(),
        payment.getAmount(),
        payment.getCurrency(),
        payment.getPgPaymentKey(),
        payment.getPgTransactionId(),
        payment.getApprovedAt()
    );
  }

  /** 결제 조회(상세) 응답 DTO */
  public PaymentResponseDto toPaymentResponseDto(Payment payment) {
    if (payment == null) return null;

    return new PaymentResponseDto(
        payment.getPaymentId(),
        payment.getOrderId(),
        payment.getStatus().name(),
        payment.getAmount(),
        payment.getCurrency(),
        payment.getMethod(),
        payment.getProvider(),
        payment.getPgPaymentKey(),
        payment.getPgTransactionId(),
        payment.getRequestedAt(),
        payment.getApprovedAt(),
        payment.getFailedAt(),
        payment.getFailReason()
    );
  }

  /**
   * 결제 이벤트(사실 기록) 생성
   * - 웹훅을 안 써도 "confirm 요청이 들어왔음" 같은 이벤트를 남기고 싶을 때 사용
   * - payloadJson은 서비스에서 ObjectMapper로 만든 raw json 문자열을 넣는 걸 추천
   */
  public PaymentEvent toClientConfirmEvent(Payment payment, String payloadJson) {
    String safePayload = (payloadJson == null) ? "{}" : payloadJson;
    return PaymentEvent.of(payment, PaymentEventType.CLIENT_CONFIRM_REQUEST, safePayload);
  }

  /** (선택) 웹훅 이벤트 기록용 */
  public PaymentEvent toWebhookEvent(Payment payment, PaymentEventType type, String payloadJson) {
    String safePayload = (payloadJson == null) ? "{}" : payloadJson;
    return PaymentEvent.of(payment, type, safePayload);
  }
}
