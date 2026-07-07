package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service.InventoryService;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.Order;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.exception.OrderErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentEventType;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.PgApproveResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.PaymentRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.validator.PaymentStateValidator;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * confirmPayment 흐름의 DB 작업을 트랜잭션 단위로 분리.
 * PaymentCommandService.confirmPayment()는 @Transactional 없이 이 헬퍼를 호출하며,
 * PG API 호출은 두 트랜잭션 사이에서 커넥션 없이 실행됨. (P-4 패턴)
 */
@Service
@RequiredArgsConstructor
public class PaymentConfirmHelper {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final InventoryService inventoryService;
  private final PaymentEventService paymentEventService;
  private final PaymentStateValidator paymentStateValidator;
  private final ObjectMapper objectMapper;

  /**
   * TX1: 비관적 락 조회 → 소유 검증 → READY 검증 → CONFIRMING 전환 → 이벤트 저장 → 커밋.
   * 이 메서드가 반환되면 DB 커넥션이 반환됨.
   *
   * 이미 CAPTURED 상태라면(직전 confirm 응답 전 네트워크 단절 후 재시도) 상태 전환 없이
   * 그대로 반환한다 — 호출부에서 PG 재호출 없이 기존 결제 정보로 성공 응답을 만든다. (P-12)
   */
  @Transactional
  public Payment lockAndMarkConfirming(Long paymentId, Long userId, String confirmEventJson) {
    Payment payment = paymentRepository.findByIdWithLock(paymentId)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

    orderRepository.findByOrderIdAndUser_UserId(payment.getOrderId(), userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    if (payment.getStatus() == PaymentStatus.CAPTURED) {
      return payment;
    }

    paymentStateValidator.validateConfirmable(payment);
    payment.markConfirming();

    paymentEventService.saveEvent(payment, PaymentEventType.CLIENT_CONFIRM_REQUEST, confirmEventJson);

    return payment;
  }

  /**
   * TX2: CONFIRMING → CAPTURED, Order PAID, 재고 차감, 이벤트 저장.
   * PG API 호출 성공 후 실행됨.
   */
  @Transactional
  public Payment capture(Long paymentId, Long orderId, PgApproveResponse resp) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

    OffsetDateTime approvedAt = parseApprovedAt(resp.getApprovedAt());
    payment.markCaptured(resp.getPaymentKey(), resp.getTransactionKey(), approvedAt);

    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    order.markPaid();

    for (OrderItem oi : order.getOrderItems()) {
      inventoryService.decreaseStock(oi.getItemVariant().getItemVariantId(), oi.getQuantity());
    }

    paymentEventService.saveEvent(payment, PaymentEventType.WEBHOOK_APPROVED, safeJson(resp));

    return payment;
  }

  /**
   * TX3: CONFIRMING → FAILED.
   * PG API 호출 실패 또는 검증 실패 시 실행됨.
   */
  @Transactional
  public void fail(Long paymentId, String reason) {
    paymentRepository.findById(paymentId).ifPresent(p -> p.markFailed(reason));
  }

  private OffsetDateTime parseApprovedAt(String approvedAt) {
    if (approvedAt == null || approvedAt.isBlank()) return null;
    try {
      return OffsetDateTime.parse(approvedAt);
    } catch (Exception e) {
      return null;
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
