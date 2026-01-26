package com.ohsooo.platform.ohsooshoppingmall.domain.cart.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartItemAddRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartItemUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.Cart;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.CartItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.exception.CartErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.mapper.CartMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository.CartItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository.CartRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final CartMapper cartMapper;

  /**
   * 내 장바구니 조회
   */
  @Transactional(readOnly = true)
  public CartResponseDto getMyCart(Long userId) {
    validateAuthPrincipal(userId);

    Cart cart = cartRepository.findWithItemsByUser_UserId(userId)
        .orElseGet(() -> createCartIfNotExists(userId));

    return cartMapper.toCartResponseDto(cart);
  }

  /**
   * 장바구니 상품 추가
   */
  public CartResponseDto addItem(Long userId, CartItemAddRequestDto request) {
    validateAuthPrincipal(userId);

    Cart cart = getOrCreateCart(userId);

    ItemVariant itemVariant = itemVariantRepository.findById(request.getItemVariantId())
        .orElseThrow(() -> new BusinessException(CartErrorCode.ITEM_VARIANT_NOT_FOUND));

    CartItem newItem = CartItem.of(itemVariant, request.getQuantity());
    cart.addOrIncreaseItem(newItem);

    // 저장은 cascade로 되지만, 조회 응답은 옵션까지 필요하니 fetch(EntityGraph) 버전으로 다시 로딩
    Cart reloaded = cartRepository.findWithItemsByUser_UserId(userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

    return cartMapper.toCartResponseDto(reloaded);
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
   */
  public void clearCart(Long userId) {
    validateAuthPrincipal(userId);

    Cart cart = cartRepository.findByUser_UserId(userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

    cart.clear();
  }

  /* ==================== 내부 유틸 ==================== */

  private Cart getOrCreateCart(Long userId) {
    return cartRepository.findByUser_UserId(userId)
        .orElseGet(() -> createCartIfNotExists(userId));
  }

  private Cart createCartIfNotExists(Long userId) {
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
