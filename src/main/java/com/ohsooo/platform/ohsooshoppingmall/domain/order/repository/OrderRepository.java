package com.ohsooo.platform.ohsooshoppingmall.domain.order.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  /**
   * 내 주문 목록 조회(요약용)
   * - summary에서 "첫 상품명 + 옵션"을 만들려면
   *   orderItems.itemVariant.item + itemVariantOptions.option 까지 로딩하는 게 안전함.
   */
  @EntityGraph(attributePaths = {
      "orderItems",
      "orderItems.itemVariant",
      "orderItems.itemVariant.item",
      "orderItems.itemVariant.itemVariantOptions",
      "orderItems.itemVariant.itemVariantOptions.option"
  })
  List<Order> findWithItemsByUser_UserIdOrderByOrderIdDesc(Long userId);

  /**
   * 내 주문 상세 조회
   * - 상세에서 상품/옵션까지 내려주려면 동일하게 풀로 로딩
   */
  @EntityGraph(attributePaths = {
      "orderItems",
      "orderItems.itemVariant",
      "orderItems.itemVariant.item",
      "orderItems.itemVariant.itemVariantOptions",
      "orderItems.itemVariant.itemVariantOptions.option"
  })
  Optional<Order> findByOrderIdAndUser_UserId(Long orderId, Long userId);

  /**
   * 결제 생성(createPayment) 동시 요청 직렬화용 락 조회.
   * 동일 주문에 대한 createPayment 호출이 겹칠 때, 뒤 요청이 앞 요청의 커밋을 기다리게 해
   * 중복 READY Payment 생성을 막는다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.user.userId = :userId")
  Optional<Order> findByOrderIdAndUser_UserIdWithLock(
      @Param("orderId") Long orderId, @Param("userId") Long userId);
}
