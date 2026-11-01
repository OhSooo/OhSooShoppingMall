package com.ohsooo.platform.ohsooshoppingmall.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service.InventoryService;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.Order;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItemStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.mapper.OrderMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderItemHistoryRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto.PricingResult;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 결제 전 주문 취소 확정 시 쿠폰 복구 / 금액 음수 방지 / 재고 미변경 검증. (#72)
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceCancelTest {

  private static final Long USER_ID = 1L;
  private static final Long ORDER_ID = 100L;
  private static final Long USER_COUPON_ID = 50L;

  @Mock private OrderItemRepository orderItemRepository;
  @Mock private OrderItemHistoryRepository orderItemHistoryRepository;
  @Mock private InventoryService inventoryService;
  @Mock private UserCouponService userCouponService;
  @Mock private OrderMapper orderMapper;

  @InjectMocks private OrderService orderService;

  private ItemVariant variant(Long id) {
    ItemVariant variant = mock(ItemVariant.class);
    lenient().when(variant.getItemVariantId()).thenReturn(id);
    return variant;
  }

  /** 상품 2개(각 45,000원)짜리 주문. 쿠폰 할인액을 주면 쿠폰 주문으로 만든다. */
  private Order order(BigDecimal discount, Long userCouponId, OrderItem... items) {
    Order order = Order.create(mock(User.class), "김민지", "010-0000-0000", "서울시", null, null, null);
    ReflectionTestUtils.setField(order, "orderId", ORDER_ID);
    for (OrderItem item : items) {
      order.addOrderItem(item);
    }
    BigDecimal total = BigDecimal.valueOf(45000L * items.length);
    order.applyPricing(new PricingResult(total, discount, BigDecimal.ZERO, total.subtract(discount)));
    order.applyCoupon(userCouponId);
    return order;
  }

  private OrderItem item(Long variantId) {
    OrderItem item = OrderItem.of(variant(variantId), 1, BigDecimal.valueOf(45000), "사료", "5KG");
    item.changeStatus(OrderItemStatus.CANCEL_REQUESTED);
    return item;
  }

  private void stubFind(Long orderItemId, OrderItem item) {
    when(orderItemRepository.findByOrderItemIdAndOrder_User_UserId(orderItemId, USER_ID))
        .thenReturn(Optional.of(item));
  }

  @Test
  void 결제전_쿠폰주문을_전체취소하면_쿠폰을_복구하고_금액은_0_재고는_그대로다() {
    OrderItem only = item(10L);
    Order order = order(BigDecimal.valueOf(5000), USER_COUPON_ID, only);
    stubFind(1L, only);

    orderService.confirmCancelOrderItem(USER_ID, 1L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(order.getDiscountAmount()).isEqualByComparingTo("0");
    assertThat(order.getFinalPrice()).isEqualByComparingTo("0");
    assertThat(order.getUserCouponId()).isEqualTo(USER_COUPON_ID);
    verify(userCouponService).releaseByOrder(ORDER_ID);
    verify(inventoryService, never()).increaseStock(anyLong(), anyInt());
  }

  @Test
  void 결제전_쿠폰주문을_부분취소하면_쿠폰은_유지되고_재고는_그대로다() {
    OrderItem first = item(10L);
    OrderItem second = OrderItem.of(variant(20L), 1, BigDecimal.valueOf(45000), "사료", "10KG");
    Order order = order(BigDecimal.valueOf(5000), USER_COUPON_ID, first, second);
    stubFind(1L, first);

    orderService.confirmCancelOrderItem(USER_ID, 1L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_CANCELED);
    assertThat(order.getOriginalTotalPrice()).isEqualByComparingTo("45000");
    assertThat(order.getFinalPrice()).isEqualByComparingTo("40000");
    verify(userCouponService, never()).releaseByOrder(anyLong());
    verify(inventoryService, never()).increaseStock(anyLong(), anyInt());
  }

  @Test
  void 부분취소로_남은금액보다_할인이_커져도_금액은_0_미만이_되지_않는다() {
    OrderItem first = item(10L);
    OrderItem second = OrderItem.of(variant(20L), 1, BigDecimal.valueOf(45000), "사료", "10KG");
    Order order = order(BigDecimal.valueOf(60000), USER_COUPON_ID, first, second);
    stubFind(1L, first);

    orderService.confirmCancelOrderItem(USER_ID, 1L);

    assertThat(order.getFinalPrice()).isEqualByComparingTo("0");
  }

  @Test
  void 쿠폰없는_결제전_주문을_전체취소하면_쿠폰_복구를_호출하지_않는다() {
    OrderItem only = item(10L);
    Order order = order(BigDecimal.ZERO, null, only);
    stubFind(1L, only);

    orderService.confirmCancelOrderItem(USER_ID, 1L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(order.getFinalPrice()).isEqualByComparingTo("0");
    verify(userCouponService, never()).releaseByOrder(anyLong());
    verify(inventoryService, never()).increaseStock(anyLong(), anyInt());
  }

  @Test
  void 결제가_끝난_주문의_취소는_기존대로_재고를_복원한다() {
    OrderItem only = item(10L);
    Order order = order(BigDecimal.ZERO, null, only);
    order.markPaid();
    stubFind(1L, only);

    orderService.confirmCancelOrderItem(USER_ID, 1L);

    verify(inventoryService).increaseStock(10L, 1);
    verify(userCouponService, never()).releaseByOrder(anyLong());
  }
}
