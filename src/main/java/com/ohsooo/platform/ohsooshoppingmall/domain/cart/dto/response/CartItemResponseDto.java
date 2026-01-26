package com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartItemResponseDto {

  // 장바구니 식별
  private Long cartItemId;
  private Long itemVariantId;

  // 화면 표시용
  private String itemName;
  private int price;
  private List<String> options; // ex) ["SIZE: M", "COLOR: Black"]

  private int quantity;
  private boolean saleable; // 판매 가능 여부
}
