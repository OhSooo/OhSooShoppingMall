package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.exception.OrderErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.RefundCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.RefundResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Refund;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.RefundItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.mapper.RefundMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.PaymentRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.RefundItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.RefundRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.validator.RefundValidator;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
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

  /** 환불 생성(REQUESTED) */
  public RefundResponseDto createRefund(Long userId, Long paymentId, RefundCreateRequestDto request) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);
    if (paymentId == null) throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
    if (request == null) throw new BusinessException(PaymentErrorCode.INVALID_REFUND_AMOUNT);

    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

    // 주문 소유 검증
    orderRepository.findByOrderIdAndUser_UserId(payment.getOrderId(), userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    // 최소 정책 검증: 환불 금액 양수
    refundValidator.validateAmount(request.getAmount());

    // Refund 생성 및 저장
    Refund refund = refundMapper.toRefundEntity(payment, request);
    Refund savedRefund = refundRepository.save(refund);

    // RefundItem들 생성 후 한 번에 저장
    List<RefundItem> items = refundMapper.toRefundItemEntities(savedRefund, request);
    if (items != null && !items.isEmpty()) {
      refundItemRepository.saveAll(items);
    }

    return refundMapper.toRefundResponseDto(savedRefund);
  }
}
