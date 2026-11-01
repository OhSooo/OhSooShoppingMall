package com.ohsooo.platform.ohsooshoppingmall.domain.pricing.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto.PricingLineItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto.PricingResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

  public PricingResult calculate(List<PricingLineItem> lineItems) {
    return calculate(lineItems, BigDecimal.ZERO);
  }

  /**
   * 할인액을 반영해 주문 금액을 계산한다.
   * - discountAmount: 서버가 상품 합계 기준으로 계산한 할인액(쿠폰 등). 0 이상, 상품 합계 이하여야 한다.
   * - 할인 후 최종 금액이 0원이 되는 것은 허용한다.
   */
  public PricingResult calculate(List<PricingLineItem> lineItems, BigDecimal discountAmount) {
    BigDecimal originalTotal = lineItems.stream()
        .map(li -> li.variant().getPrice().multiply(BigDecimal.valueOf(li.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (discountAmount == null
        || discountAmount.signum() < 0
        || discountAmount.compareTo(originalTotal) > 0) {
      throw new IllegalArgumentException(
          "discountAmount must be between 0 and originalTotal. discount=" + discountAmount
              + " originalTotal=" + originalTotal);
    }

    BigDecimal deliveryFee = BigDecimal.ZERO;
    BigDecimal finalPrice = originalTotal.subtract(discountAmount).add(deliveryFee);

    return new PricingResult(originalTotal, discountAmount, deliveryFee, finalPrice);
  }
}
