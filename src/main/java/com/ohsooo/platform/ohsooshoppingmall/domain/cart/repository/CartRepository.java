package com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

  Optional<Cart> findByUser_UserId(Long userId);

  /**
   * 장바구니 조회 시 cartItems + itemVariant + item + itemVariantOptions + option 까지 한 번에 로딩
   * -> CartMapper에서 itemName / price / options 를 안전하게 만들 수 있음 (N+1 방지)
   */
  @EntityGraph(attributePaths = {
      "cartItems",
      "cartItems.itemVariant",
      "cartItems.itemVariant.item",
      "cartItems.itemVariant.itemVariantOptions",
      "cartItems.itemVariant.itemVariantOptions.option"
  })
  Optional<Cart> findWithItemsByUser_UserId(Long userId);
}
