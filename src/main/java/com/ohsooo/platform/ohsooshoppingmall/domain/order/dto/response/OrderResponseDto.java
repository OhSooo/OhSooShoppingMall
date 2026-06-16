package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponseDto {

  private Long orderId;
  private Long userId;
  private String status;

  private BigDecimal originalTotalPrice;
  private BigDecimal discountAmount;
  private BigDecimal deliveryFee;
  private BigDecimal finalPrice;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  private ShippingInfo shipping;
  private List<OrderItemResponseDto> items;

  @Getter
  @AllArgsConstructor
  public static class ShippingInfo {
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private String shippingPostcode;
    private String shippingAddressDetail;
    private String requestNote;
  }
}
