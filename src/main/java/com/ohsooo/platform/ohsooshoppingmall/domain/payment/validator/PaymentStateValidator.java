package com.ohsooo.platform.ohsooshoppingmall.domain.payment.validator;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 결제 상태 전이를 안전하게 만들기 위한 Validator.
 *
 * 사용 시점:
 * - PaymentCommandService에서 confirm(승인) 요청 처리 전/후 상태 체크에 사용
 */
@Component
public class PaymentStateValidator {

  public void validateConfirmable(Payment payment) {
    if (payment == null) {
      throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    PaymentStatus status = payment.getStatus();

    // READY에서만 confirm 허용
    if (status != PaymentStatus.READY) {
      throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_CONFIRMABLE);
    }
  }

  public void validateNotAlreadyCaptured(Payment payment) {
    if (payment.getStatus() == PaymentStatus.CAPTURED) {
      throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_CAPTURED);
    }
  }

  public void validateNotAlreadyFailed(Payment payment) {
    if (payment.getStatus() == PaymentStatus.FAILED) {
      throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_FAILED);
    }
  }
}
