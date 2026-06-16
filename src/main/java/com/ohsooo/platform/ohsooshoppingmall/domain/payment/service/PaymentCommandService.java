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
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.mapper.PaymentMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.PgClientRouter;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.client.PgClient;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request.TossApproveRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.PgApproveResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.PaymentRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;

  private final PaymentMapper paymentMapper;
  private final PaymentConfirmHelper confirmHelper;
  private final PgClientRouter pgClientRouter;

  private final ObjectMapper objectMapper;

  /** 결제 생성(READY) */
  @Transactional
  public PaymentCreateResponseDto createPayment(Long userId, PaymentCreateRequestDto request) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);
    if (request == null) throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);

    if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
    }

    Order order = orderRepository.findByOrderIdAndUser_UserId(request.getOrderId(), userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    if (request.getAmount().compareTo(order.getFinalPrice()) != 0) {
      throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
    }

    // [P-2] CONFIRMING도 포함 — PG 호출 중인 결제가 있으면 중복 생성 차단
    if (paymentRepository.existsByOrderIdAndStatusIn(
        request.getOrderId(),
        List.of(PaymentStatus.READY, PaymentStatus.CONFIRMING, PaymentStatus.CAPTURED))) {
      throw new BusinessException(PaymentErrorCode.DUPLICATE_PAYMENT);
    }

    Payment payment = paymentMapper.toReadyPaymentEntity(request);
    Payment saved = paymentRepository.save(payment);

    return paymentMapper.toCreateResponseDto(saved, request);
  }

  /**
   * 결제 확정(confirm).
   *
   * @Transactional 없음 — DB 커넥션을 PG API 호출 동안 잡지 않기 위해 (P-4).
   * TX1(lockAndMarkConfirming) → PG API 호출 → TX2(capture) or TX3(fail) 순으로 실행됨.
   */
  public PaymentConfirmResponseDto confirmPayment(
      Long userId,
      Long paymentId,
      PaymentConfirmRequestDto request
  ) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);
    if (paymentId == null) throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
    if (request == null) throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);

    // KRW 정수 형식 검증 (fast fail, DB 접근 전)
    if (request.getAmount() != null) {
      toKrwIntegerAmount(request.getAmount());
    }

    String confirmEventJson = toConfirmPayloadJson(paymentId, request);

    // TX1: 비관적 락 조회 → 소유 검증 → CONFIRMING 전환 → 커밋 → 커넥션 반환
    Payment payment = confirmHelper.lockAndMarkConfirming(paymentId, userId, confirmEventJson);

    // 클라이언트 금액 vs DB 저장 금액 검증 (detached entity의 단순 필드 읽기 — 안전)
    if (request.getAmount() != null && payment.getAmount() != null) {
      if (payment.getAmount().compareTo(request.getAmount()) != 0) {
        confirmHelper.fail(payment.getPaymentId(), "Amount mismatch between request and stored payment");
        throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
      }
    }

    // PG API 호출 — 트랜잭션 밖, DB 커넥션 미점유
    PgClient pgClient = pgClientRouter.route(payment.getProvider());
    int tossAmount = toKrwIntegerAmount(payment.getAmount());

    TossApproveRequest approveRequest = new TossApproveRequest(
        request.getPaymentKey(),
        String.valueOf(payment.getOrderId()),
        tossAmount
    );

    try {
      PgApproveResponse resp = pgClient.approve(approveRequest);

      if (resp == null || resp.getTotalAmount() == null) {
        throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED);
      }
      BigDecimal approvedAmount = BigDecimal.valueOf(resp.getTotalAmount());
      if (payment.getAmount().compareTo(approvedAmount) != 0) {
        throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
      }

      // TX2: CAPTURED + Order PAID + 재고 차감
      Payment captured = confirmHelper.capture(payment.getPaymentId(), payment.getOrderId(), resp);
      return paymentMapper.toConfirmResponseDto(captured);

    } catch (BusinessException be) {
      confirmHelper.fail(payment.getPaymentId(), be.getMessage());
      throw be;
    } catch (Exception e) {
      confirmHelper.fail(payment.getPaymentId(), e.getMessage());
      throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED, e);
    }
  }

  private int toKrwIntegerAmount(BigDecimal amount) {
    if (amount == null) throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
    if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
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
}
