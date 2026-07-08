package com.ohsooo.platform.ohsooshoppingmall.domain.cart.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.CartLineItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartItemAddRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartItemUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartMergeRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartMergeRequestDto.CartMergeItemDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartMergeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartMergeSkippedItemDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.exception.CartErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.mapper.CartMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository.CartRedisRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.response.StockResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service.InventoryService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

  private final CartRedisRepository cartRedisRepository;
  private final ItemVariantRepository itemVariantRepository;
  private final InventoryService inventoryService;
  private final CartMapper cartMapper;

  /**
   * 내 장바구니 조회
   * - 카트가 없으면(Redis에 키가 없으면) 빈 배열 반환
   */
  public CartResponseDto getMyCart(Long userId) {
    validateAuthPrincipal(userId);
    return loadCart(userId);
  }

  /**
   * 장바구니 상품 추가
   * - 이미 담긴 variant면 수량 증가(원자적 HINCRBY), 없으면 신규 추가
   */
  public CartResponseDto addItem(Long userId, CartItemAddRequestDto request) {
    validateAuthPrincipal(userId);

    ItemVariant itemVariant = itemVariantRepository.findById(request.getItemVariantId())
        .orElseThrow(() -> new BusinessException(CartErrorCode.ITEM_VARIANT_NOT_FOUND));

    if (itemVariant.getStatus() == ItemVariantStatus.DISABLED) {
      throw new BusinessException(CartErrorCode.ITEM_DISABLED);
    }
    if (itemVariant.getStatus() == ItemVariantStatus.OUT_OF_STOCK) {
      throw new BusinessException(CartErrorCode.OUT_OF_STOCK);
    }

    cartRedisRepository.increaseQuantity(userId, request.getItemVariantId(), request.getQuantity());

    return loadCart(userId);
  }

  /**
   * 비로그인 장바구니 병합
   * - 프론트엔드 로컬(비로그인) 장바구니 목록을 로그인 시점에 서버 Cart로 병합
   * - 정책: 수량 합산. 품절/판매중지/재고초과 항목은 전체를 실패시키지 않고 해당 항목만 스킵 처리 후 사유와 함께 응답
   */
  public CartMergeResponseDto mergeGuestCart(Long userId, CartMergeRequestDto request) {
    validateAuthPrincipal(userId);

    List<CartMergeSkippedItemDto> skippedItems = new ArrayList<>();

    for (CartMergeItemDto item : request.getItems()) {
      try {
        ItemVariant itemVariant = itemVariantRepository.findById(item.getItemVariantId())
            .orElseThrow(() -> new BusinessException(CartErrorCode.ITEM_VARIANT_NOT_FOUND));

        if (itemVariant.getStatus() == ItemVariantStatus.DISABLED) {
          throw new BusinessException(CartErrorCode.ITEM_DISABLED);
        }
        if (itemVariant.getStatus() == ItemVariantStatus.OUT_OF_STOCK) {
          throw new BusinessException(CartErrorCode.OUT_OF_STOCK);
        }

        int existingQuantity = cartRedisRepository.findQuantity(userId, item.getItemVariantId()).orElse(0);
        int mergedQuantity = existingQuantity + item.getQuantity();

        StockResponse stock = inventoryService.getStock(item.getItemVariantId());
        if (mergedQuantity > stock.getQuantity()) {
          throw new BusinessException(CartErrorCode.EXCEEDS_STOCK);
        }

        cartRedisRepository.increaseQuantity(userId, item.getItemVariantId(), item.getQuantity());

      } catch (BusinessException e) {
        BaseErrorCode errorCode = e.getErrorCode();
        skippedItems.add(new CartMergeSkippedItemDto(
            item.getItemVariantId(), errorCode.getCode(), errorCode.getMessage()));
      }
    }

    return new CartMergeResponseDto(loadCart(userId), skippedItems);
  }

  /**
   * 장바구니 상품 수량 변경
   */
  public CartResponseDto updateItemQuantity(
      Long userId,
      Long itemVariantId,
      CartItemUpdateRequestDto request
  ) {
    validateAuthPrincipal(userId);

    if (request.getQuantity() == null || request.getQuantity() <= 0) {
      throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
    }

    if (!cartRedisRepository.exists(userId, itemVariantId)) {
      throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
    }

    StockResponse stock = inventoryService.getStock(itemVariantId);
    if (request.getQuantity() > stock.getQuantity()) {
      throw new BusinessException(CartErrorCode.EXCEEDS_STOCK);
    }

    cartRedisRepository.setQuantity(userId, itemVariantId, request.getQuantity());

    return loadCart(userId);
  }

  /**
   * 장바구니 상품 삭제
   */
  public void removeItem(Long userId, Long itemVariantId) {
    validateAuthPrincipal(userId);

    if (!cartRedisRepository.exists(userId, itemVariantId)) {
      throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
    }

    cartRedisRepository.removeItem(userId, itemVariantId);
  }

  /**
   * 장바구니 비우기
   * - 카트가 없어도 "비워진 상태"이므로 성공(멱등)
   */
  public void clearCart(Long userId) {
    validateAuthPrincipal(userId);
    cartRedisRepository.clear(userId);
  }

  /* ==================== Order 도메인용 ==================== */

  /**
   * 장바구니 전체 조회 (Order의 CART_ALL 주문 생성용)
   */
  public List<CartLineItem> getCartLineItems(Long userId) {
    validateAuthPrincipal(userId);
    Map<Long, Integer> quantities = cartRedisRepository.findAll(userId);
    return toLineItems(quantities);
  }

  /**
   * 장바구니 중 지정한 itemVariantId만 조회 (Order의 CART_SELECTED 주문 생성용)
   * - 요청한 itemVariantId 중 하나라도 카트에 없으면 예외(O-3: 조용히 무시하지 않고 전체 실패)
   */
  public List<CartLineItem> getCartLineItems(Long userId, Collection<Long> itemVariantIds) {
    validateAuthPrincipal(userId);

    Map<Long, Integer> quantities = new LinkedHashMap<>();
    for (Long itemVariantId : itemVariantIds) {
      int quantity = cartRedisRepository.findQuantity(userId, itemVariantId)
          .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));
      quantities.put(itemVariantId, quantity);
    }
    return toLineItems(quantities);
  }

  /**
   * 지정한 itemVariantId들만 장바구니에서 제거 (Order 생성 후 정리용)
   */
  public void removeItems(Long userId, Collection<Long> itemVariantIds) {
    validateAuthPrincipal(userId);
    cartRedisRepository.removeItems(userId, itemVariantIds);
  }

  /* ==================== 내부 유틸 ==================== */

  private CartResponseDto loadCart(Long userId) {
    Map<Long, Integer> quantities = cartRedisRepository.findAll(userId);
    List<ItemVariant> variants = itemVariantRepository.findWithOptionsByItemVariantIdIn(quantities.keySet());
    return cartMapper.toCartResponseDto(userId, quantities, variants);
  }

  private List<CartLineItem> toLineItems(Map<Long, Integer> quantities) {
    if (quantities.isEmpty()) return List.of();

    List<ItemVariant> variants = itemVariantRepository.findWithOptionsByItemVariantIdIn(quantities.keySet());
    Map<Long, ItemVariant> variantById = variants.stream()
        .collect(Collectors.toMap(ItemVariant::getItemVariantId, v -> v));

    List<CartLineItem> result = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
      ItemVariant variant = variantById.get(entry.getKey());
      if (variant == null) continue; // 카트에 담긴 뒤 상품이 삭제된 경우는 조용히 제외
      result.add(new CartLineItem(variant, entry.getValue()));
    }
    return result;
  }

  private void validateAuthPrincipal(Long userId) {
    if (userId == null) {
      throw new BusinessException(CartErrorCode.AUTH_PRINCIPAL_MISSING);
    }
  }
}
