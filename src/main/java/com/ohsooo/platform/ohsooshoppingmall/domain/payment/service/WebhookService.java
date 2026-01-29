package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentEventType;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
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

  public void handleTossWebhook(HttpServletRequest request, String rawBody) {
    // webhook-secret이 설정된 경우만 검증, 없으면 verifier 내부에서 "패스"하도록 구현했을 거라고 가정
    tossWebhookVerifier.verify(request, rawBody);

    // 지금 단계: payload를 파싱해서 상태를 바꾸기까지는 안 하고
    // "이벤트 저장"까지만 해도 충분
    // (나중에 배포/HTTPS 된 후 토스 스펙 확정되면 매핑하면 됨)

    // paymentKey를 파싱해서 payment를 찾는 로직은 토스 payload 스펙 확정 후 붙이자.
    // 당장 필수는 아님.

    // 혹시 지금도 저장하고 싶으면, paymentKey를 rawBody에서 꺼내는 로직을 추가해야 함.
    // (payload 구조를 네가 보내주면 그 자리에서 정확히 넣어줄게)

    // 임시: 그냥 원문 저장을 위한 더미 처리(결제 매핑 없이도 이벤트 테이블은 쌓을 수 있음)
    // -> 하지만 PaymentEvent는 payment FK가 필수라서, 매핑 없이는 저장 불가.
    // 그래서 현재 단계에서는 "검증만" 하고 종료하는게 안전.
  }

  /** payload에서 paymentKey를 뽑아낼 수 있게 되면, 아래 같은 흐름으로 붙이면 됨 */
  private Payment findPaymentOrThrow(String paymentKey) {
    return paymentRepository.findByPgPaymentKey(paymentKey)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
  }

  private void saveApprovedEvent(Payment payment, String rawBody) {
    paymentEventService.saveEvent(payment, PaymentEventType.WEBHOOK_APPROVED, rawBody);
  }
}
