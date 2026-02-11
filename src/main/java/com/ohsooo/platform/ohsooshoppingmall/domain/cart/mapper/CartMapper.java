package com.ohsooo.platform.ohsooshoppingmall.domain.cart.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartItemOptionResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartItemResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.Cart;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.CartItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantOption;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

  public CartResponseDto toCartResponseDto(Cart cart) {
    if (cart == null) return null;

    List<CartItemResponseDto> items = toCartItemResponseDtoList(cart.getCartItems());
    int totalPrice = calculateTotalPrice(items);

    return new CartResponseDto(
        cart.getCartId(),
        cart.getUser().getUserId(),
        totalPrice,
        items
    );
  }

  public CartItemResponseDto toCartItemResponseDto(CartItem cartItem) {
    if (cartItem == null) return null;

    ItemVariant variant = cartItem.getItemVariant();
    Item item = variant.getItem();

    Set<CartItemOptionResponseDto> options = toOptionDtos(variant.getItemVariantOptions());

    boolean saleable =
        item.getStatus() == ItemStatus.ACTIVE
            && variant.getStatus() == ItemVariantStatus.ACTIVE;

    return new CartItemResponseDto(
        cartItem.getCartItemId(),
        variant.getItemVariantId(),
        item.getName(),
        variant.getPrice(),
        options,
        cartItem.getQuantity(),
        saleable
    );
  }

  public List<CartItemResponseDto> toCartItemResponseDtoList(List<CartItem> cartItems) {
    if (cartItems == null || cartItems.isEmpty()) return Collections.emptyList();

    return cartItems.stream()
        .map(this::toCartItemResponseDto)
        .collect(Collectors.toList());
  }

  private Set<CartItemOptionResponseDto> toOptionDtos(Set<ItemVariantOption> itemVariantOptions) {
    if (itemVariantOptions == null || itemVariantOptions.isEmpty()) {
      return Collections.emptySet();
    }

    return itemVariantOptions.stream()
        .map(ItemVariantOption::getOption)
        .map(this::toOptionDto)
        .collect(Collectors.toSet());
  }

  private CartItemOptionResponseDto toOptionDto(Option option) {
    return new CartItemOptionResponseDto(option.getType(), option.getValue());
  }

  private int calculateTotalPrice(List<CartItemResponseDto> items) {
    if (items == null || items.isEmpty()) return 0;

    int sum = 0;
    for (CartItemResponseDto i : items) {
      // 판매 불가 상품은 합계에서 제외하고 싶으면 여기서 조건 걸면 됨.
      // 지금은 "장바구니 총합"이니까 일단 모두 합산.
      sum += i.getPrice() * i.getQuantity();
    }
    return sum;
  }
}
