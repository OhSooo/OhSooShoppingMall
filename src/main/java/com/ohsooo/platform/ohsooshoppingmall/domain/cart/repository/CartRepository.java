package com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

  Optional<Cart> findByUser_UserId(Long userId);

  @EntityGraph(attributePaths = {"cartItems", "cartItems.itemVariant"})
  Optional<Cart> findWithItemsByUser_UserId(Long userId);
}
