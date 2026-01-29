package com.ohsooo.platform.ohsooshoppingmall.domain.payment.validator;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 환불 생성 요청 검증기.
 *
 * 사용 시점:
 * - RefundCommandService(또는 PaymentCommandService에서 환불 생성 기능)에서 환불 요청을 받으면 호출
 *
 * 현재(MVP):
 * - 환불 기능 본격 구현 전이라 최소 검증만 제공
 */
@Component
public class RefundValidator {

  public void validateAmount(Integer refundAmount) {
    if (refundAmount == null || refundAmount <= 0) {
      throw new BusinessException(PaymentErrorCode.INVALID_REFUND_AMOUNT);
    }
  }
}
