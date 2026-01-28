package com.ohsooo.platform.ohsooshoppingmall.domain.order.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  @EntityGraph(attributePaths = {
      "order",
      "itemVariant",
      "itemVariant.item",
      "itemVariant.itemVariantOptions",
      "itemVariant.itemVariantOptions.option"
  })
  Optional<OrderItem> findByOrderItemIdAndOrder_User_UserId(Long orderItemId, Long userId);
}
