package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.exception.OrderErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.RefundCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.RefundResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Refund;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.RefundItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.mapper.RefundMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.PgClientRouter;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.client.PgClient;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request.TossCancelRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.PaymentRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.RefundItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.RefundRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.validator.RefundValidator;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundCommandService {

  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final RefundItemRepository refundItemRepository;

  private final OrderRepository orderRepository;

  private final RefundMapper refundMapper;
  private final RefundValidator refundValidator;
  private final PgClientRouter pgClientRouter;

  /** 환불 생성 — Toss 취소 API 호출까지 포함 */
  public RefundResponseDto createRefund(Long userId, Long paymentId, RefundCreateRequestDto request) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);
    if (paymentId == null) throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
    if (request == null) throw new BusinessException(PaymentErrorCode.INVALID_REFUND_AMOUNT);

    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

    // 주문 소유 검증
    orderRepository.findByOrderIdAndUser_UserId(payment.getOrderId(), userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    // [P-7] CAPTURED 상태일 때만 환불 가능
    if (payment.getStatus() != PaymentStatus.CAPTURED) {
      throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_REFUNDABLE);
    }

    // [P-8] 기본 금액 검증 (양수, KRW 정수)
    refundValidator.validateAmount(request.getAmount());

    // [P-8] 기존 환불 누적 합산 후 초과 여부 검증
    BigDecimal alreadyRefunded = refundRepository.sumSucceededAmountByPaymentId(paymentId);
    refundValidator.validateRefundableAmount(request.getAmount(), payment.getAmount(), alreadyRefunded);

    // Refund 생성 및 저장 (REQUESTED)
    Refund refund = refundMapper.toRefundEntity(payment, request);
    Refund savedRefund = refundRepository.save(refund);

    // RefundItem 저장
    List<RefundItem> items = refundMapper.toRefundItemEntities(savedRefund, request);
    if (items != null && !items.isEmpty()) {
      refundItemRepository.saveAll(items);
    }

    // [P-6] Toss 취소 API 호출
    PgClient pgClient = pgClientRouter.route(payment.getProvider());
    TossCancelRequest cancelRequest = new TossCancelRequest(
        request.getReason(),
        request.getAmount().intValueExact()
    );

    try {
      pgClient.cancel(payment.getPgPaymentKey(), cancelRequest);

      savedRefund.markSucceeded();

      // 전액 환불이면 Payment를 REFUNDED로 전환
      BigDecimal totalRefunded = alreadyRefunded.add(request.getAmount());
      if (totalRefunded.compareTo(payment.getAmount()) == 0) {
        payment.markRefunded();
      }

    } catch (BusinessException be) {
      savedRefund.markFailed();
      throw be;
    } catch (Exception e) {
      savedRefund.markFailed();
      throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED, e);
    }

    return refundMapper.toRefundResponseDto(savedRefund);
  }
}
