package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.exception.OrderErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.RefundDetailResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Refund;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.RefundItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.mapper.PaymentMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.mapper.RefundMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.PaymentRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.RefundItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.RefundRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {

  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final RefundItemRepository refundItemRepository;

  private final OrderRepository orderRepository;

  private final PaymentMapper paymentMapper;
  private final RefundMapper refundMapper;

  public PaymentResponseDto getMyPayment(Long userId, Long paymentId) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);

    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

    orderRepository.findByOrderIdAndUser_UserId(payment.getOrderId(), userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    return paymentMapper.toPaymentResponseDto(payment);
  }

  public PaymentResponseDto getMyPaymentByOrderId(Long userId, Long orderId) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);

    orderRepository.findByOrderIdAndUser_UserId(orderId, userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    Payment payment = paymentRepository.findByOrderId(orderId)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

    return paymentMapper.toPaymentResponseDto(payment);
  }

  public RefundDetailResponseDto getMyRefundDetail(Long userId, Long refundId) {
    if (userId == null) throw new BusinessException(PaymentErrorCode.AUTH_PRINCIPAL_MISSING);

    Refund refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new BusinessException(PaymentErrorCode.REFUND_NOT_FOUND));

    Payment payment = refund.getPayment();

    orderRepository.findByOrderIdAndUser_UserId(payment.getOrderId(), userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    List<RefundItem> items = refundItemRepository.findAllByRefund_RefundId(refundId);

    return refundMapper.toRefundDetailResponseDto(refund, items);
  }
}
