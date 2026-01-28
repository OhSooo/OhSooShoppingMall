package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderCreateResponseDto {

  /**
   * 생성된 주문 ID
   */
  private Long orderId;

  /**
   * 주문 상태 (CREATED/PAID/...)
   */
  private String status;

  /**
   * 총 결제 예정 금액 (주문 생성 시점 확정)
   */
  private int totalPrice;

  /**
   * 주문서 화면에 뿌릴 상품 목록
   */
  private List<OrderItemResponseDto> items;

  /**
   * (선택) 결제 페이지로 넘어갈 때 참고할 결제 시도 식별값 등을
   * 나중에 확장 가능 (현재는 null로 둬도 됨)
   */
  private String paymentRedirectHint;
}
