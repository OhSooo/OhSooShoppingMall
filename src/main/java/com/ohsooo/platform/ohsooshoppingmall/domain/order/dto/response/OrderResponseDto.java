package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponseDto {

  private Long orderId;          // 필수
  private Long userId;           // 필수
  private String status;         // 필수
  private int totalPrice;        // 필수
  private OffsetDateTime createdAt; // 필수
  private OffsetDateTime updatedAt; // 필수

  /** 배송 정보(주문 스냅샷) */
  private ShippingInfo shipping; // 필수(orders에 NOT NULL로 넣었으니까)

  /** 주문 상세 상품 목록 */
  private List<OrderItemResponseDto> items; // 필수

  @Getter
  @AllArgsConstructor
  public static class ShippingInfo {
    private String receiverName;           // 필수
    private String receiverPhone;          // 필수
    private String shippingAddress;        // 필수
    private String shippingPostcode;       // 선택
    private String shippingAddressDetail;  // 선택
    private String requestNote;            // 선택
  }
}
