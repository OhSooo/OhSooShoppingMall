package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentEventType;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.TossWebhookPayload;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.webhook.TossWebhookVerifier;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.PaymentRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WebhookService {

  private final TossWebhookVerifier tossWebhookVerifier;
  private final PaymentRepository paymentRepository;
  private final PaymentEventService paymentEventService;
  private final ObjectMapper objectMapper;

  public void handleTossWebhook(HttpServletRequest request, String rawBody) {
    // [P-10] HMAC-SHA256 서명 검증 — 실패 시 PG_SIGNATURE_INVALID(403) 예외
    tossWebhookVerifier.verify(request, rawBody);

    // payload에서 paymentKey 추출
    TossWebhookPayload payload = parsePayload(rawBody);
    if (payload.getData() == null || payload.getData().getPaymentKey() == null) {
      return;
    }

    // paymentKey로 Payment 조회 후 이벤트 저장
    Payment payment = paymentRepository.findByPgPaymentKey(payload.getData().getPaymentKey())
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

    paymentEventService.saveEvent(payment, PaymentEventType.WEBHOOK_APPROVED, rawBody);
  }

  private TossWebhookPayload parsePayload(String rawBody) {
    try {
      return objectMapper.readValue(rawBody, TossWebhookPayload.class);
    } catch (Exception e) {
      throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED, e);
    }
  }
}
