package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StockAdjustRequest {

  /**
   * 조정할 재고 수량.
   * - 절대값으로 "설정"하고 싶으면 InventoryService에서 set 방식으로 처리
   * - 증감(delta) 방식이면 이 값을 delta로 해석 (+10 / -3)
   *
   * MVP에선 안전하게 delta를 양수만 받아서 "입고"만 허용해도 됨.
   */
  @NotNull(message = "quantity는 필수입니다.")
  @Min(value = 0, message = "quantity는 0 이상이어야 합니다.")
  private Integer quantity;
}
