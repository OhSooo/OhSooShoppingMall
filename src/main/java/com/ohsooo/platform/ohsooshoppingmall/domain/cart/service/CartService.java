package com.ohsooo.platform.ohsooshoppingmall.domain.cart.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartItemAddRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartItemUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartMergeRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartMergeRequestDto.CartMergeItemDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartMergeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartMergeSkippedItemDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.Cart;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.CartItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.exception.CartErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.mapper.CartMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository.CartItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository.CartRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.response.StockResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service.InventoryService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ItemVariantRepository itemVariantRepository;
  private final UserRepository userRepository;
  private final InventoryService inventoryService;
  private final CartMapper cartMapper;

  /**
   * 내 장바구니 조회
   * - Cart가 없으면 DB에 생성하지 않고 빈 배열 반환
   */
  @Transactional(readOnly = true)
  public CartResponseDto getMyCart(Long userId) {
    validateAuthPrincipal(userId);

    return cartRepository.findWithItemsByUser_UserId(userId)
        .map(cartMapper::toCartResponseDto)
        .orElseGet(() -> new CartResponseDto(null, userId, BigDecimal.ZERO, Collections.emptyList()));
  }

  /**
   * 장바구니 상품 추가
   * - Cart가 없으면 이 시점에 생성
   * - 동시 요청으로 UNIQUE(user_id) 충돌이 나면 재조회로 복구
   */
  public CartResponseDto addItem(Long userId, CartItemAddRequestDto request) {
    validateAuthPrincipal(userId);

    Cart cart = getOrCreateCart(userId);

    ItemVariant itemVariant = itemVariantRepository.findById(request.getItemVariantId())
        .orElseThrow(() -> new BusinessException(CartErrorCode.ITEM_VARIANT_NOT_FOUND));

    if (itemVariant.getStatus() == ItemVariantStatus.DISABLED) {
      throw new BusinessException(CartErrorCode.ITEM_DISABLED);
    }
    if (itemVariant.getStatus() == ItemVariantStatus.OUT_OF_STOCK) {
      throw new BusinessException(CartErrorCode.OUT_OF_STOCK);
    }

    CartItem newItem = CartItem.of(itemVariant, request.getQuantity());
    cart.addOrIncreaseItem(newItem);

    // 옵션까지 포함한 응답을 위해 EntityGraph 조회로 다시 로딩
    Cart reloaded = cartRepository.findWithItemsByUser_UserId(userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

    return cartMapper.toCartResponseDto(reloaded);
  }

  /**
   * 비로그인 장바구니 병합
   * - 프론트엔드 로컬(비로그인) 장바구니 목록을 로그인 시점에 서버 Cart로 병합
   * - 정책: 수량 합산(기존 addOrIncreaseItem과 동일). 품절/판매중지/재고초과 항목은
   *   전체를 실패시키지 않고 해당 항목만 스킵 처리 후 사유와 함께 응답
   */
  public CartMergeResponseDto mergeGuestCart(Long userId, CartMergeRequestDto request) {
    validateAuthPrincipal(userId);

    Cart cart = getOrCreateCart(userId);
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

        CartItem existing = cart.findItemByVariantId(item.getItemVariantId());
        int existingQuantity = existing != null ? existing.getQuantity() : 0;
        int mergedQuantity = existingQuantity + item.getQuantity();

        StockResponse stock = inventoryService.getStock(item.getItemVariantId());
        if (mergedQuantity > stock.getQuantity()) {
          throw new BusinessException(CartErrorCode.EXCEEDS_STOCK);
        }

        cart.addOrIncreaseItem(CartItem.of(itemVariant, item.getQuantity()));

      } catch (BusinessException e) {
        BaseErrorCode errorCode = e.getErrorCode();
        skippedItems.add(new CartMergeSkippedItemDto(
            item.getItemVariantId(), errorCode.getCode(), errorCode.getMessage()));
      }
    }

    Cart reloaded = cartRepository.findWithItemsByUser_UserId(userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

    return new CartMergeResponseDto(cartMapper.toCartResponseDto(reloaded), skippedItems);
  }

  /**
   * 장바구니 상품 수량 변경
   */
  public CartResponseDto updateItemQuantity(
      Long userId,
      Long cartItemId,
      CartItemUpdateRequestDto request
  ) {
    validateAuthPrincipal(userId);

    if (request.getQuantity() == null || request.getQuantity() <= 0) {
      throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
    }

    CartItem cartItem = cartItemRepository
        .findByCartItemIdAndCart_User_UserId(cartItemId, userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));

    StockResponse stock = inventoryService.getStock(cartItem.getItemVariant().getItemVariantId());
    if (request.getQuantity() > stock.getQuantity()) {
      throw new BusinessException(CartErrorCode.EXCEEDS_STOCK);
    }

    cartItem.changeQuantity(request.getQuantity());

    Cart reloaded = cartRepository.findWithItemsByUser_UserId(userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

    return cartMapper.toCartResponseDto(reloaded);
  }

  /**
   * 장바구니 상품 삭제
   */
  public void removeItem(Long userId, Long cartItemId) {
    validateAuthPrincipal(userId);

    CartItem cartItem = cartItemRepository
        .findByCartItemIdAndCart_User_UserId(cartItemId, userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));

    cartItem.getCart().removeItemByVariantId(
        cartItem.getItemVariant().getItemVariantId()
    );
  }

  /**
   * 장바구니 비우기
   * - Cart가 없어도 "비워진 상태"이므로 성공(멱등)
   */
  public void clearCart(Long userId) {
    validateAuthPrincipal(userId);

    cartRepository.findByUser_UserId(userId).ifPresent(Cart::clear);
  }

  /* ==================== 내부 유틸 ==================== */

  private Cart getOrCreateCart(Long userId) {
    return cartRepository.findByUser_UserId(userId)
        .orElseGet(() -> {
          try {
            return createCart(userId);
          } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 UNIQUE(user_id) 충돌이 날 수 있음 -> 누군가 먼저 생성했으니 재조회
            log.warn("Cart create raced for userId={}, retrying find. cause={}", userId, e.getMessage());
            return cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> e);
          }
        });
  }

  private Cart createCart(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ACCESS_DENIED));

    Cart cart = Cart.create(user);
    return cartRepository.save(cart);
  }

  private void validateAuthPrincipal(Long userId) {
    if (userId == null) {
      throw new BusinessException(CartErrorCode.AUTH_PRINCIPAL_MISSING);
    }
  }
}
