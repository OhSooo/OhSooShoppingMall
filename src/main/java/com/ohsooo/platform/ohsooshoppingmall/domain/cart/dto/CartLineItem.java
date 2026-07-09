package com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;

public record CartLineItem(ItemVariant variant, int quantity) {}
