package com.ohsooo.platform.ohsooshoppingmall.domain.cart.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartItemOptionResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartItemResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantOption;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

  /**
   * Redis에서 읽어온 (itemVariantId -> quantity)와 Catalog에서 배치 조회한 ItemVariant 목록을 조합해 응답을 만든다.
   * quantities에는 있지만 variants에 없는 itemVariantId(카트에 담긴 뒤 상품이 삭제된 경우)는 조용히 제외한다.
   */
  public CartResponseDto toCartResponseDto(Long userId, Map<Long, Integer> quantities, List<ItemVariant> variants) {
    Map<Long, ItemVariant> variantById = variants.stream()
        .collect(Collectors.toMap(ItemVariant::getItemVariantId, v -> v));

    List<CartItemResponseDto> items = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
      ItemVariant variant = variantById.get(entry.getKey());
      if (variant == null) continue;
      items.add(toCartItemResponseDto(variant, entry.getValue()));
    }

    BigDecimal totalPrice = calculateTotalPrice(items);
    return new CartResponseDto(userId, totalPrice, items);
  }

  public CartItemResponseDto toCartItemResponseDto(ItemVariant variant, int quantity) {
    Item item = variant.getItem();

    Set<CartItemOptionResponseDto> options = toOptionDtos(variant.getItemVariantOptions());

    boolean saleable =
        item.getStatus() == ItemStatus.ACTIVE
            && variant.getStatus() == ItemVariantStatus.ACTIVE;

    return new CartItemResponseDto(
        variant.getItemVariantId(),
        item.getName(),
        variant.getPrice(),
        item.getStore().getStoreId(),
        item.getStore().getName(),
        options,
        quantity,
        saleable
    );
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

  private BigDecimal calculateTotalPrice(List<CartItemResponseDto> items) {
    if (items == null || items.isEmpty()) return BigDecimal.ZERO;

    BigDecimal sum = BigDecimal.ZERO;
    for (CartItemResponseDto i : items) {
      sum = sum.add(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
    }
    return sum;
  }
}
