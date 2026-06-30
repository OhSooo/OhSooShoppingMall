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
  public Payment toReadyPaymentEntity(PaymentCreateRequestDto request, String tossOrderId) {
    if (request == null) return null;

    return Payment.ready(
        request.getOrderId(),
        request.getAmount(),
        request.getCurrency(),
        request.getMethod(),
        request.getProvider(),
        tossOrderId
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
        request != null ? request.getCustomerName() : null,
        payment.getTossOrderId()
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

  public PaymentEvent toClientConfirmEvent(Payment payment, String payloadJson) {
    String safePayload = (payloadJson == null) ? "{}" : payloadJson;
    return PaymentEvent.of(payment, PaymentEventType.CLIENT_CONFIRM_REQUEST, safePayload);
  }

  public PaymentEvent toWebhookEvent(Payment payment, PaymentEventType type, String payloadJson) {
    String safePayload = (payloadJson == null) ? "{}" : payloadJson;
    return PaymentEvent.of(payment, type, safePayload);
  }
}
