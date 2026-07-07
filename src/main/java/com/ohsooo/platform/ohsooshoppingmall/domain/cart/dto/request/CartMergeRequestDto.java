package com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartMergeRequestDto {

  @NotEmpty(message = "items는 비어있을 수 없습니다.")
  @Valid
  private List<CartMergeItemDto> items;

  @Getter
  @NoArgsConstructor
  public static class CartMergeItemDto {

    @NotNull(message = "itemVariantId는 필수입니다.")
    private Long itemVariantId;

    @NotNull(message = "quantity는 필수입니다.")
    @Min(value = 1, message = "quantity는 1 이상이어야 합니다.")
    private Integer quantity;
  }
}
