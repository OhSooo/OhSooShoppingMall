package com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response;

import java.util.List;
import java.util.Set;
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

  // A안: 옵션 응답 형태
  private Set<CartItemOptionResponseDto> options;

  private int quantity;
  private boolean saleable; // 판매 가능 여부
}
