package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemResponseDto {

  private Long orderItemId;

  private Long itemId;        // 상세 페이지 이동에 유용
  private Long itemVariantId; // 재고/옵션 단위
  private String sku;         // variant 식별(디버깅/정산/CS에서도 도움됨)

  private String itemName;

  /**
   * 구매 당시 가격 스냅샷 (ItemVariant.price를 확정 저장한 값)
   */
  private int priceAtPurchase;

  private int quantity;

  /**
   * 주문 당시 선택 옵션
   * - 네 OptionType(SIZE/COLOR) 그대로 사용
   */
  private List<OrderItemOptionResponseDto> options;

  /**
   * 상태(UI에 그대로 표시)
   */
  private String status;

  /**
   * 구매 가능 여부(주문 생성 시점 검증 결과 or 현재 상태 기준)
   * - 주문서 화면에서 "품절/판매중지" 표시가 필요할 때 유용
   */
  private boolean saleable;

  /**
   * 지금 취소 버튼 활성화 가능한지
   * - 서비스 정책에 따라 계산해서 내려주면 프론트가 편함
   */
  private boolean cancelable;
}
