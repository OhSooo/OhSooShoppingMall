package com.ohsooo.platform.ohsooshoppingmall.domain.order.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
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
        if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
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

  /** 배송 필수값: 최종적으로 Order.create()에 들어갈 값이 null이면 안 됨 */
  public void validateShippingRequiredFields(User user, OrderCreateRequestDto request) {
    OrderCreateRequestDto.ShippingInfo s = request.getShipping();

    String receiverName = (s != null) ? s.getReceiverName() : null;
    String receiverPhone = (s != null) ? s.getReceiverPhone() : null;
    String shippingAddress = (s != null) ? s.getShippingAddress() : null;

    // fallback: user 기본값
    if (receiverName == null) receiverName = user.getName();
    if (receiverPhone == null) receiverPhone = user.getPhone();
    if (shippingAddress == null) shippingAddress = user.getAddress();

    if (receiverName == null || receiverPhone == null || shippingAddress == null) {
      throw new BusinessException(OrderErrorCode.SHIPPING_REQUIRED_FIELDS_MISSING);
    }
  }

  public void validateDirectItems(List<OrderItemCreateRequestDto> items) {
    for (OrderItemCreateRequestDto dto : items) {
      if (dto.getQuantity() <= 0) {
        throw new BusinessException(OrderErrorCode.INVALID_QUANTITY);
      }
    }
  }
}
