package com.ohsooo.platform.ohsooshoppingmall.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.service.CartService;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.exception.InventoryErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service.InventoryService;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto.OrderSource;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderItemCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.Order;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.mapper.OrderMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.service.PaymentCommandService;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.service.PricingService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 주문 생성 시 쿠폰 할인 적용/사용 처리와 0원 주문 결제 완료 처리 검증. (#68)
 * - 금액 계산은 실제 PricingService, 저장되는 Order는 실제 엔티티를 사용해 최종 상태를 확인한다.
 * - 트랜잭션 롤백 자체는 단위 테스트로 증명할 수 없으므로 OrderCouponIntegrationTest(PostgreSQL)에서 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceCouponTest {

  private static final Long USER_ID = 1L;
  private static final Long USER_COUPON_ID = 50L;
  private static final Long ORDER_ID = 100L;
  private static final Long VARIANT_ID = 10L;

  @Mock private OrderRepository orderRepository;
  @Mock private UserRepository userRepository;
  @Mock private CartService cartService;
  @Mock private ItemVariantRepository itemVariantRepository;
  @Mock private InventoryService inventoryService;
  @Mock private OrderMapper orderMapper;
  @Mock private OrderValidator orderValidator;
  @Mock private UserCouponService userCouponService;
  @Mock private PaymentCommandService paymentCommandService;
  @Spy private PricingService pricingService = new PricingService();

  @InjectMocks private OrderService orderService;

  private Order order;

  @BeforeEach
  void setUp() {
    TransactionSynchronizationManager.initSynchronization();

    User user = mock(User.class);
    order = Order.create(user, "김민지", "010-0000-0000", "서울시", null, null, null);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    lenient().when(orderMapper.toOrderEntity(any(), any())).thenReturn(order);
    lenient().when(orderRepository.save(order)).thenAnswer(inv -> {
      ReflectionTestUtils.setField(order, "orderId", ORDER_ID);
      return order;
    });

    // 상품 합계 30,000원 (10,000원 x 3개)
    ItemVariant variant = mock(ItemVariant.class);
    lenient().when(variant.getItemVariantId()).thenReturn(VARIANT_ID);
    lenient().when(variant.getPrice()).thenReturn(BigDecimal.valueOf(10000));
    when(itemVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));
  }

  @AfterEach
  void tearDown() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private OrderCreateRequestDto request(Long userCouponId) {
    return new OrderCreateRequestDto(OrderSource.DIRECT, null,
        List.of(new OrderItemCreateRequestDto(VARIANT_ID, 3)), null, null, userCouponId);
  }

  @Test
  void 쿠폰_없이_주문하면_쿠폰_호출_없이_기존_금액과_CREATED_상태로_생성된다() {
    orderService.createOrder(USER_ID, request(null));

    assertThat(order.getOriginalTotalPrice()).isEqualByComparingTo("30000");
    assertThat(order.getDiscountAmount()).isEqualByComparingTo("0");
    assertThat(order.getFinalPrice()).isEqualByComparingTo("30000");
    assertThat(order.getUserCouponId()).isNull();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

    verify(userCouponService, never()).calculateDiscount(anyLong(), anyLong(), any());
    verify(userCouponService, never()).redeem(anyLong(), anyLong(), anyLong(), any(), any());
    verify(paymentCommandService, never()).createZeroAmountPayment(anyLong());
    verify(inventoryService, never()).decreaseStock(anyLong(), anyInt());
  }

  @Test
  void 쿠폰을_적용하면_서버_상품합계로_할인을_계산하고_주문_저장_후_같은_금액으로_redeem한다() {
    when(userCouponService.calculateDiscount(USER_ID, USER_COUPON_ID, new BigDecimal("30000")))
        .thenReturn(BigDecimal.valueOf(5000));

    orderService.createOrder(USER_ID, request(USER_COUPON_ID));

    assertThat(order.getOriginalTotalPrice()).isEqualByComparingTo("30000");
    assertThat(order.getDiscountAmount()).isEqualByComparingTo("5000");
    assertThat(order.getFinalPrice()).isEqualByComparingTo("25000");
    assertThat(order.getUserCouponId()).isEqualTo(USER_COUPON_ID);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

    InOrder inOrder = inOrder(userCouponService, orderRepository);
    inOrder.verify(userCouponService).calculateDiscount(USER_ID, USER_COUPON_ID, new BigDecimal("30000"));
    inOrder.verify(orderRepository).save(order);
    inOrder.verify(userCouponService).redeem(
        USER_ID, USER_COUPON_ID, ORDER_ID, new BigDecimal("30000"), BigDecimal.valueOf(5000));

    verify(paymentCommandService, never()).createZeroAmountPayment(anyLong());
    verify(inventoryService, never()).decreaseStock(anyLong(), anyInt());
  }

  @Test
  void 쿠폰_검증에_실패하면_주문을_저장하지_않는다() {
    when(userCouponService.calculateDiscount(USER_ID, USER_COUPON_ID, new BigDecimal("30000")))
        .thenThrow(new BusinessException(CouponErrorCode.USER_COUPON_ALREADY_USED));

    assertThatThrownBy(() -> orderService.createOrder(USER_ID, request(USER_COUPON_ID)))
        .isInstanceOf(BusinessException.class);

    verify(orderRepository, never()).save(any());
    verify(userCouponService, never()).redeem(anyLong(), anyLong(), anyLong(), any(), any());
  }

  @Test
  void redeem이_실패하면_예외를_삼키지_않고_전파한다() {
    when(userCouponService.calculateDiscount(USER_ID, USER_COUPON_ID, new BigDecimal("30000")))
        .thenReturn(BigDecimal.valueOf(5000));
    when(userCouponService.redeem(USER_ID, USER_COUPON_ID, ORDER_ID, new BigDecimal("30000"), BigDecimal.valueOf(5000)))
        .thenThrow(new BusinessException(CouponErrorCode.USER_COUPON_ALREADY_USED));

    assertThatThrownBy(() -> orderService.createOrder(USER_ID, request(USER_COUPON_ID)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void 할인_후_0원이면_PG_없이_결제행을_만들고_PAID로_전환한_뒤_마지막에_재고를_차감한다() {
    when(userCouponService.calculateDiscount(USER_ID, USER_COUPON_ID, new BigDecimal("30000")))
        .thenReturn(BigDecimal.valueOf(30000));

    orderService.createOrder(USER_ID, request(USER_COUPON_ID));

    assertThat(order.getFinalPrice()).isEqualByComparingTo("0");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

    InOrder inOrder = inOrder(userCouponService, paymentCommandService, orderMapper, inventoryService);
    inOrder.verify(userCouponService).redeem(
        USER_ID, USER_COUPON_ID, ORDER_ID, new BigDecimal("30000"), BigDecimal.valueOf(30000));
    inOrder.verify(paymentCommandService).createZeroAmountPayment(ORDER_ID);
    inOrder.verify(orderMapper).toCreateResponseDto(order);
    inOrder.verify(inventoryService).decreaseStock(VARIANT_ID, 3);
  }

  @Test
  void 최종_0원_주문에서_재고가_부족하면_예외를_전파한다() {
    when(userCouponService.calculateDiscount(USER_ID, USER_COUPON_ID, new BigDecimal("30000")))
        .thenReturn(BigDecimal.valueOf(30000));
    doThrow(new BusinessException(InventoryErrorCode.INSUFFICIENT_STOCK))
        .when(inventoryService).decreaseStock(VARIANT_ID, 3);

    assertThatThrownBy(() -> orderService.createOrder(USER_ID, request(USER_COUPON_ID)))
        .isInstanceOf(BusinessException.class);
  }
}
