package com.ohsooo.platform.ohsooshoppingmall.domain.cart.mapper;

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
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

  public CartResponseDto toCartResponseDto(Cart cart) {
    if (cart == null) return null;

    return new CartResponseDto(
        cart.getCartId(),
        cart.getUser().getUserId(),
        toCartItemResponseDtoList(cart.getCartItems())
    );
  }

  public CartItemResponseDto toCartItemResponseDto(CartItem cartItem) {
    if (cartItem == null) return null;

    ItemVariant variant = cartItem.getItemVariant();
    Item item = variant.getItem();

    // 옵션 문자열 조합
    List<String> options = variant.getItemVariantOptions() == null
        ? Collections.emptyList()
        : variant.getItemVariantOptions().stream()
            .map(ItemVariantOption::getOption)
            .map(this::formatOption)
            .collect(Collectors.toList());

    boolean saleable =
        item.getStatus() == ItemStatus.ACTIVE &&
            variant.getStatus() == ItemVariantStatus.ACTIVE;

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

  private String formatOption(Option option) {
    return option.getType().name() + ": " + option.getValue();
  }
}
