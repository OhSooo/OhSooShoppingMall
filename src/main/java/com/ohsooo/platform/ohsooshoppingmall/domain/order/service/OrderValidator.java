package com.ohsooo.platform.ohsooshoppingmall.domain.order.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderItemCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.exception.OrderErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {

  public void validateCreateRequest(OrderCreateRequestDto request) {
    if (request == null || request.getSource() == null) {
      throw new BusinessException(OrderErrorCode.INVALID_ORDER_SOURCE);
    }

    switch (request.getSource()) {
      case CART_ALL -> { /* ok */ }
      case CART_SELECTED -> {
        if (request.getItemVariantIds() == null || request.getItemVariantIds().isEmpty()) {
          throw new BusinessException(OrderErrorCode.INVALID_CART_ITEM_IDS);
        }
      }
      case DIRECT -> {
        if (request.getItems() == null || request.getItems().isEmpty()) {
          throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);
        }
      }
      default -> throw new BusinessException(OrderErrorCode.INVALID_ORDER_SOURCE);
    }
  }

  /**
   * 배송 필수값 검증: 요청에 담긴 값만 사용하며, 서버가 회원 프로필 등으로 채우지 않는다.
   * API 요청은 @Valid(@NotNull/@NotBlank)에서 먼저 걸러지지만, 서비스 직접 호출 시에도
   * DB NOT NULL 위반(500) 대신 400으로 거부되도록 한 번 더 확인한다.
   */
  public void validateShippingRequiredFields(OrderCreateRequestDto request) {
    OrderCreateRequestDto.ShippingInfo s = request.getShipping();
    if (s == null
        || isBlank(s.getReceiverName())
        || isBlank(s.getReceiverPhone())
        || isBlank(s.getShippingAddress())) {
      throw new BusinessException(OrderErrorCode.SHIPPING_REQUIRED_FIELDS_MISSING);
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public void validateDirectItems(List<OrderItemCreateRequestDto> items) {
    for (OrderItemCreateRequestDto dto : items) {
      if (dto.getQuantity() <= 0) {
        throw new BusinessException(OrderErrorCode.INVALID_QUANTITY);
      }
    }
  }
}
