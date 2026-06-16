package com.ohsooo.platform.ohsooshoppingmall.domain.pricing.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto.PricingLineItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto.PricingResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

  public PricingResult calculate(List<PricingLineItem> lineItems) {
    BigDecimal originalTotal = lineItems.stream()
        .map(li -> li.variant().getPrice().multiply(BigDecimal.valueOf(li.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal discountAmount = BigDecimal.ZERO;
    BigDecimal deliveryFee = BigDecimal.ZERO;
    BigDecimal finalPrice = originalTotal.subtract(discountAmount).add(deliveryFee);

    return new PricingResult(originalTotal, discountAmount, deliveryFee, finalPrice);
  }
}
