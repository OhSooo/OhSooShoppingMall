package com.ohsooo.platform.ohsooshoppingmall.domain.order.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantOption;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.*;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.Order;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItemStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

  public Order toOrderEntity(User user, OrderCreateRequestDto request) {
    OrderCreateRequestDto.ShippingInfo s = (request != null) ? request.getShipping() : null;

    String receiverName = (s != null) ? s.getReceiverName() : null;
    String receiverPhone = (s != null) ? s.getReceiverPhone() : null;
    String shippingAddress = (s != null) ? s.getShippingAddress() : null;
    String shippingPostcode = (s != null) ? s.getShippingPostcode() : null;
    String shippingAddressDetail = (s != null) ? s.getShippingAddressDetail() : null;
    String requestNote = (s != null) ? s.getRequestNote() : null;

    return Order.create(
        user,
        receiverName,
        receiverPhone,
        shippingAddress,
        shippingPostcode,
        shippingAddressDetail,
        requestNote
    );
  }

  public OrderCreateResponseDto toCreateResponseDto(Order order) {
    List<OrderItemResponseDto> items = new ArrayList<>();
    if (order.getOrderItems() != null) {
      for (OrderItem oi : order.getOrderItems()) {
        items.add(toOrderItemResponseDto(oi));
      }
    }

    return new OrderCreateResponseDto(
        order.getOrderId(),
        order.getStatus().name(),
        order.getOriginalTotalPrice(),
        order.getDiscountAmount(),
        order.getDeliveryFee(),
        order.getFinalPrice(),
        items,
        null
    );
  }

  public OrderResponseDto toOrderResponseDto(Order order, List<OrderItemResponseDto> items) {
    OrderResponseDto.ShippingInfo shipping = new OrderResponseDto.ShippingInfo(
        order.getReceiverName(),
        order.getReceiverPhone(),
        order.getShippingAddress(),
        order.getShippingPostcode(),
        order.getShippingAddressDetail(),
        order.getShippingRequestNote()
    );

    return new OrderResponseDto(
        order.getOrderId(),
        (order.getUser() != null ? order.getUser().getUserId() : null),
        order.getStatus().name(),
        order.getOriginalTotalPrice(),
        order.getDiscountAmount(),
        order.getDeliveryFee(),
        order.getFinalPrice(),
        order.getCreatedAt(),
        order.getUpdatedAt(),
        shipping,
        items
    );
  }

  public OrderItemResponseDto toOrderItemResponseDto(OrderItem oi) {
    ItemVariant v = oi.getItemVariant();
    Item item = (v != null) ? v.getItem() : null;

    Long itemId = (item != null) ? item.getItemId() : null;
    Long variantId = (v != null) ? v.getItemVariantId() : null;
    String sku = (v != null) ? v.getSku() : null;
    String itemName = (item != null) ? item.getName() : null;

    List<OrderItemOptionResponseDto> optionDtos = new ArrayList<>();
    if (v != null && v.getItemVariantOptions() != null) {
      for (ItemVariantOption ivo : v.getItemVariantOptions()) {
        Option opt = (ivo != null) ? ivo.getOption() : null;
        if (opt != null) {
          optionDtos.add(new OrderItemOptionResponseDto(opt.getType().name(), opt.getValue()));
        }
      }
    }

    boolean saleable = true;
    boolean cancelable = isCancelable(oi.getStatus());

    return new OrderItemResponseDto(
        oi.getOrderItemId(),
        itemId,
        variantId,
        sku,
        itemName,
        oi.getPriceAtPurchase(),
        oi.getQuantity(),
        optionDtos,
        oi.getStatus().name(),
        saleable,
        cancelable
    );
  }

  private boolean isCancelable(OrderItemStatus status) {
    if (status == null) return false;
    return status != OrderItemStatus.CANCELED
        && status != OrderItemStatus.REFUNDED
        && status != OrderItemStatus.CANCEL_REQUESTED
        && status != OrderItemStatus.REFUND_REQUESTED;
  }
}
