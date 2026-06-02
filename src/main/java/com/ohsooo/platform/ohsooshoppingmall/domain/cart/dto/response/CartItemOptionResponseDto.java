package com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartItemOptionResponseDto {

  private OptionType type;   // SIZE / COLOR
  private String value;      // "M", "Black" ...
}
