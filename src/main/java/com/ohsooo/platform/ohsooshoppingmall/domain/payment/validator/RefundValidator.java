package com.ohsooo.platform.ohsooshoppingmall.domain.payment.validator;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 환불 생성 요청 검증기.
 */
@Component
public class RefundValidator {

  public void validateAmount(BigDecimal refundAmount) {
    if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(PaymentErrorCode.INVALID_REFUND_AMOUNT);
    }

    // KRW 기준: 소수점 금액 방지(정책적으로 안전)
    if (refundAmount.stripTrailingZeros().scale() > 0) {
      throw new BusinessException(PaymentErrorCode.INVALID_REFUND_AMOUNT);
    }
  }
}
