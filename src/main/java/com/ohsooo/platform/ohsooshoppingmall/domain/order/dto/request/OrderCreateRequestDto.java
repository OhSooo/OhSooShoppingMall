package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

  /**
   * [필수] 주문 생성 소스
   * - CART_ALL: 내 장바구니 전체 주문
   * - CART_SELECTED: cartItemIds로 지정한 항목만 주문
   * - DIRECT: items로 직접 주문(바로구매)
   */
  @NotNull
  private OrderSource source;

  /**
   * [조건부] CART_SELECTED일 때 사용
   */
  @Size(max = 200)
  private List<Long> cartItemIds;

  /**
   * [조건부] DIRECT일 때 사용
   */
  @Valid
  @Size(max = 50)
  private List<OrderItemCreateRequestDto> items;

  /**
   * [선택] 주문 메모/요청사항
   * - orders.shipping_request_note로 합쳐서 넣거나
   * - memo 컬럼을 따로 두고 싶으면 orders에 memo 추가(선택)
   */
  @Size(max = 255)
  private String memo;

  /**
   * [선택] 배송 정보
   * - null이면 서버가 User 기본 배송지/프로필에서 채워서 주문 생성
   * - 주문 스냅샷으로 orders 테이블에 저장
   */
  @Valid
  private ShippingInfo shipping;

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShippingInfo {

    /**
     * [shipping 제공 시 필수 권장]
     */
    @NotBlank
    @Size(max = 255)
    private String receiverName;

    /**
     * [shipping 제공 시 필수 권장]
     */
    @NotBlank
    @Size(max = 50)
    private String receiverPhone;

    /**
     * [shipping 제공 시 필수 권장]
     */
    @NotBlank
    @Size(max = 255)
    private String shippingAddress;

    /**
     * [선택] 우편번호
     */
    @Size(max = 20)
    private String shippingPostcode;

    /**
     * [선택] 상세주소
     */
    @Size(max = 255)
    private String shippingAddressDetail;

    /**
     * [선택] 배송 요청사항
     * - memo와 분리해서 운영할지(추천: requestNote만 쓰고 memo는 주문메모로)
     * - 둘 다 받을지 정책만 정하면 됨
     */
    @Size(max = 255)
    private String requestNote;
  }

  public enum OrderSource {
    CART_ALL,
    CART_SELECTED,
    DIRECT
  }
}
