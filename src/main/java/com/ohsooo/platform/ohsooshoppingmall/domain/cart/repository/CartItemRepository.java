package com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

  /**
   * cartId에 속한 장바구니 상품 목록 조회
   */
  List<CartItem> findByCart_CartId(Long cartId);

  /**
   * 한 장바구니(cart) 안에서 특정 itemVariant가 이미 담겨있는지 확인
   * - "있으면 수량 증가" 정책에 필요
   */
  Optional<CartItem> findByCart_CartIdAndItemVariant_ItemVariantId(Long cartId, Long itemVariantId);

  /**
   * cartItemId로 삭제할 때, userId 검증이 필요하면 이런 조회도 자주 씀
   * (서비스에서 "내 장바구니 아이템이 맞나?" 체크용)
   */
  Optional<CartItem> findByCartItemIdAndCart_User_UserId(Long cartItemId, Long userId);
}
