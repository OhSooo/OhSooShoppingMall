package com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto;

import java.math.BigDecimal;

public record PricingResult(
    BigDecimal originalTotal,
    BigDecimal discountAmount,
    BigDecimal deliveryFee,
    BigDecimal finalPrice
) {}
