package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.exception.OrderErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.PaymentConfirmRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.PaymentCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentConfirmResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentCreateResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentEventType;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.mapper.PaymentMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.PgClientRouter;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.client.PgClient;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request.TossApproveRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.TossApproveResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.PaymentRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.validator.PaymentStateValidator;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentCommandService {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;

  private final PaymentMapper paymentMapper;
  private final PaymentStateValidator paymentStateValidator;

  private final PaymentEventService paymentEventService;
  private final PgClientRouter pgClientRouter;

  private final ObjectMapper objectMapper;

  /** 결제 생성(READY) */
  public PaymentCreateResponseDto createPayment(Long userId, PaymentCreateRequestDto request) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);
    if (request == null) throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);

    // 주문 소유 검증
    orderRepository.findByOrderIdAndUser_UserId(request.getOrderId(), userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    Payment payment = paymentMapper.toReadyPaymentEntity(request);
    Payment saved = paymentRepository.save(payment);

    return paymentMapper.toCreateResponseDto(saved, request);
  }

  /** 결제 확정(confirm) */
  public PaymentConfirmResponseDto confirmPayment(
      Long userId,
      Long paymentId,
      PaymentConfirmRequestDto request
  ) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);
    if (paymentId == null) throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
    if (request == null) throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);

    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

    // 주문 소유 검증
    orderRepository.findByOrderIdAndUser_UserId(payment.getOrderId(), userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    // 상태 검증 (READY → confirm 가능 등)
    paymentStateValidator.validateConfirmable(payment);

    // 클라이언트 confirm 요청 이벤트 기록
    paymentEventService.saveEvent(
        payment,
        PaymentEventType.CLIENT_CONFIRM_REQUEST,
        toConfirmPayloadJson(paymentId, request)
    );

    // PG 라우팅
    PgClient pgClient = pgClientRouter.route(payment.getProvider());

    TossApproveRequest approveRequest = new TossApproveRequest(
        request.getPaymentKey(),
        String.valueOf(payment.getOrderId()),
        payment.getAmount()
    );

    try {
      TossApproveResponse resp = (TossApproveResponse) pgClient.approve(approveRequest);

      // String → OffsetDateTime 변환
      OffsetDateTime approvedAt = null;
      if (resp.getApprovedAt() != null && !resp.getApprovedAt().isBlank()) {
        try {
          approvedAt = OffsetDateTime.parse(resp.getApprovedAt());
        } catch (Exception ignore) {
          approvedAt = null; // 실패하면 markCaptured 내부에서 now() 사용
        }
      }

      payment.markCaptured(
          resp.getPaymentKey(),
          resp.getTransactionKey(),
          approvedAt
      );

      // 승인 성공 이벤트 기록 (웹훅과는 별개로 내부 로그 용도)
      paymentEventService.saveEvent(payment, PaymentEventType.WEBHOOK_APPROVED, safeJson(resp));

      return paymentMapper.toConfirmResponseDto(payment);

    } catch (BusinessException be) {
      payment.markFailed(be.getMessage());
      throw be;
    } catch (Exception e) {
      payment.markFailed(e.getMessage());
      throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED, e);
    }
  }

  private String toConfirmPayloadJson(Long paymentId, PaymentConfirmRequestDto request) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("paymentId", paymentId);
      payload.put("paymentKey", request.getPaymentKey());
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      return "{\"paymentId\":" + paymentId + "}";
    }
  }

  private String safeJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      return "{}";
    }
  }
}
