package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.Order;
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
import java.math.BigDecimal;
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

    // amount null/0 이하 방지 (최소한)
    if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
    }

    // (선택) KRW 정수 정책 검증까지 하고 싶으면 열어도 됨
    // toKrwIntegerAmount(request.getAmount());

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

    // (선택) 클라이언트 amount 정책 검증: KRW 정수인지 먼저 확인
    if (request.getAmount() != null) {
      toKrwIntegerAmount(request.getAmount());
    }

    // 클라이언트가 amount를 보내면 위변조 방지로 비교
    if (request.getAmount() != null && payment.getAmount() != null) {
      if (payment.getAmount().compareTo(request.getAmount()) != 0) {
        throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
      }
    }

    // 클라이언트 confirm 요청 이벤트 기록
    paymentEventService.saveEvent(
        payment,
        PaymentEventType.CLIENT_CONFIRM_REQUEST,
        toConfirmPayloadJson(paymentId, request)
    );

    // PG 라우팅
    PgClient pgClient = pgClientRouter.route(payment.getProvider());

    // 토스 confirm amount는 "원 단위 정수" → 변환
    int tossAmount = toKrwIntegerAmount(payment.getAmount());

    TossApproveRequest approveRequest = new TossApproveRequest(
        request.getPaymentKey(),
        String.valueOf(payment.getOrderId()),
        tossAmount
    );

    try {
      TossApproveResponse resp = (TossApproveResponse) pgClient.approve(approveRequest);

      // ===== (추가) PG 승인 응답 금액 검증 =====
      // resp.totalAmount(정수) ↔ payment.amount(BigDecimal) 일치해야 함
      if (resp == null || resp.getTotalAmount() == null) {
        throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED);
      }
      BigDecimal approvedAmount = BigDecimal.valueOf(resp.getTotalAmount());
      if (payment.getAmount() == null || payment.getAmount().compareTo(approvedAmount) != 0) {
        throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
      }

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

      // 결제 확정 시 Order 상태를 PAID로 업데이트 (P-1 / O-5)
      Order order = orderRepository.findById(payment.getOrderId())
          .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
      order.markPaid();

      // 승인 성공 이벤트 기록
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

  /**
   * KRW는 소수점 없는 통화라서
   * - scale > 0 이면 예외 처리(정책적으로 더 안전)
   */
  private int toKrwIntegerAmount(BigDecimal amount) {
    if (amount == null) throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
    if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);

    // 15000.00 은 ok, 15000.10 같은 건 막기
    if (amount.stripTrailingZeros().scale() > 0) {
      throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
    }

    try {
      return amount.intValueExact();
    } catch (ArithmeticException e) {
      throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT, e);
    }
  }

  private String toConfirmPayloadJson(Long paymentId, PaymentConfirmRequestDto request) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("paymentId", paymentId);
      payload.put("paymentKey", request.getPaymentKey());
      payload.put("amount", request.getAmount());
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
