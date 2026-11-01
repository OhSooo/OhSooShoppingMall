package com.ohsooo.platform.ohsooshoppingmall.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.CartLineItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.service.CartService;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto.OrderSource;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderItemCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.Order;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.mapper.OrderMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto.PricingResult;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.service.PricingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 주문 생성 시 Redis 장바구니 삭제가 DB 커밋 이후(afterCommit)에만 실행되는지 검증한다. (#66)
 * - 실제 트랜잭션 대신 TransactionSynchronizationManager를 직접 활성화하고,
 *   등록된 synchronization의 afterCommit()을 호출해 "커밋 성공"을 흉내낸다.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceCartCleanupTest {

  private static final Long USER_ID = 1L;

  @Mock private OrderRepository orderRepository;
  @Mock private UserRepository userRepository;
  @Mock private CartService cartService;
  @Mock private ItemVariantRepository itemVariantRepository;
  @Mock private PricingService pricingService;
  @Mock private OrderMapper orderMapper;
  @Mock private OrderValidator orderValidator;

  @InjectMocks private OrderService orderService;

  private Order order;

  @BeforeEach
  void setUp() {
    TransactionSynchronizationManager.initSynchronization();

    order = mock(Order.class);
    lenient().when(order.getFinalPrice()).thenReturn(BigDecimal.valueOf(30000));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
    when(orderMapper.toOrderEntity(any(), any())).thenReturn(order);
    when(pricingService.calculate(any())).thenReturn(
        new PricingResult(BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(30000)));
  }

  @AfterEach
  void tearDown() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private ItemVariant variant(Long id) {
    ItemVariant variant = mock(ItemVariant.class);
    lenient().when(variant.getItemVariantId()).thenReturn(id);
    lenient().when(variant.getPrice()).thenReturn(BigDecimal.valueOf(10000));
    return variant;
  }

  private OrderCreateRequestDto request(OrderSource source, List<Long> itemVariantIds,
      List<OrderItemCreateRequestDto> items) {
    return new OrderCreateRequestDto(source, itemVariantIds, items, null, null, null);
  }

  private void simulateCommit() {
    for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
      sync.afterCommit();
    }
  }

  @Test
  void CART_ALL_주문은_커밋_전에는_장바구니를_지우지_않고_커밋_후_주문한_상품만_지운다() {
    List<CartLineItem> cart = List.of(
        new CartLineItem(variant(10L), 1),
        new CartLineItem(variant(20L), 2));
    when(cartService.getCartLineItems(USER_ID)).thenReturn(cart);
    when(orderRepository.save(order)).thenReturn(order);

    orderService.createOrder(USER_ID, request(OrderSource.CART_ALL, null, null));

    verify(cartService, never()).removeItems(anyLong(), anyCollection());
    verify(cartService, never()).clearCart(anyLong());

    simulateCommit();

    verify(cartService).removeItems(USER_ID, List.of(10L, 20L));
    verify(cartService, never()).clearCart(anyLong());
  }

  @Test
  void CART_SELECTED_주문은_커밋_후_선택한_상품만_지운다() {
    List<Long> selected = List.of(10L);
    List<CartLineItem> cart = List.of(new CartLineItem(variant(10L), 1));
    when(cartService.getCartLineItems(USER_ID, selected)).thenReturn(cart);
    when(orderRepository.save(order)).thenReturn(order);

    orderService.createOrder(USER_ID, request(OrderSource.CART_SELECTED, selected, null));

    verify(cartService, never()).removeItems(anyLong(), anyCollection());

    simulateCommit();

    verify(cartService).removeItems(USER_ID, List.of(10L));
  }

  @Test
  void 주문_저장이_실패하면_장바구니를_지우지_않는다() {
    List<CartLineItem> cart = List.of(new CartLineItem(variant(10L), 1));
    when(cartService.getCartLineItems(USER_ID)).thenReturn(cart);
    when(orderRepository.save(order)).thenThrow(new DataIntegrityViolationException("receiver_name null"));

    assertThatThrownBy(() -> orderService.createOrder(USER_ID, request(OrderSource.CART_ALL, null, null)))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    verify(cartService, never()).removeItems(anyLong(), anyCollection());
    verify(cartService, never()).clearCart(anyLong());
  }

  @Test
  void 커밋_후_장바구니_삭제가_실패해도_예외를_전파하지_않는다() {
    List<CartLineItem> cart = List.of(new CartLineItem(variant(10L), 1));
    when(cartService.getCartLineItems(USER_ID)).thenReturn(cart);
    when(orderRepository.save(order)).thenReturn(order);
    doThrow(new RuntimeException("redis down"))
        .when(cartService).removeItems(eq(USER_ID), anyCollection());

    orderService.createOrder(USER_ID, request(OrderSource.CART_ALL, null, null));

    assertThatCode(this::simulateCommit).doesNotThrowAnyException();
  }

  @Test
  void DIRECT_주문은_장바구니_정리를_등록하지_않는다() {
    ItemVariant v = variant(10L);
    when(itemVariantRepository.findById(10L)).thenReturn(Optional.of(v));
    when(orderRepository.save(order)).thenReturn(order);

    orderService.createOrder(USER_ID,
        request(OrderSource.DIRECT, null, List.of(new OrderItemCreateRequestDto(10L, 1))));

    assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    verify(cartService, never()).removeItems(anyLong(), anyCollection());
  }
}
