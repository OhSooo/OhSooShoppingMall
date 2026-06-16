package com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;

public record PricingLineItem(ItemVariant variant, int quantity) {}
