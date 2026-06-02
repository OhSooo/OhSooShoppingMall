package com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartItemUpdateRequestDto {

  @NotNull(message = "quantity는 필수입니다.")
  @Min(value = 1, message = "quantity는 1 이상이어야 합니다.")
  private Integer quantity;
}
