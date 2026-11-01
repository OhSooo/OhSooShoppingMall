package com.ohsooo.platform.ohsooshoppingmall.domain.order.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.CartLineItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.service.CartService;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantOption;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.CatalogErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.exception.UserErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service.InventoryService;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto.PricingLineItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.dto.PricingResult;
import com.ohsooo.platform.ohsooshoppingmall.domain.pricing.service.PricingService;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderItemCancelRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderItemCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderCreateResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderItemResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderListItemResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.Order;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderChangedBy;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItemHistory;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItemStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.exception.OrderErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.mapper.OrderMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderItemHistoryRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.repository.OrderRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.service.PaymentCommandService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderItemHistoryRepository orderItemHistoryRepository;

  private final UserRepository userRepository;
  private final CartService cartService;
  private final ItemVariantRepository itemVariantRepository;

  private final InventoryService inventoryService;
  private final PricingService pricingService;
  private final UserCouponService userCouponService;
  private final PaymentCommandService paymentCommandService;
  private final OrderMapper orderMapper;
  private final OrderValidator orderValidator;

  /** 주문 생성 */
  public OrderCreateResponseDto createOrder(Long userId, OrderCreateRequestDto request) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    orderValidator.validateCreateRequest(request);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    orderValidator.validateShippingRequiredFields(request);

    List<OrderItem> orderItems = switch (request.getSource()) {
      case CART_ALL -> buildOrderItemsFromCartAll(userId);
      case CART_SELECTED -> buildOrderItemsFromCartSelected(userId, request.getItemVariantIds());
      case DIRECT -> buildOrderItemsFromDirect(request.getItems());
    };

    if (orderItems.isEmpty()) throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);

    List<PricingLineItem> lineItems = orderItems.stream()
        .map(oi -> new PricingLineItem(oi.getItemVariant(), oi.getQuantity()))
        .toList();
    PricingResult pricing = pricingService.calculate(lineItems);

    // 쿠폰 할인: 서버가 계산한 상품 합계(배송비 제외) 기준. 클라이언트 금액은 사용하지 않는다.
    Long userCouponId = request.getUserCouponId();
    BigDecimal itemSubtotal = pricing.originalTotal();
    BigDecimal couponDiscount = BigDecimal.ZERO;
    if (userCouponId != null) {
      couponDiscount = userCouponService.calculateDiscount(userId, userCouponId, itemSubtotal);
      pricing = pricingService.calculate(lineItems, couponDiscount);
    }

    Order order = orderMapper.toOrderEntity(user, request);
    for (OrderItem oi : orderItems) {
      order.addOrderItem(oi);
    }
    order.applyPricing(pricing);
    order.applyCoupon(userCouponId);

    Order saved = orderRepository.save(order);

    // 쿠폰 사용 확정은 주문과 같은 트랜잭션에서(redeem은 MANDATORY). 예외는 삼키지 않고 전파해 함께 롤백한다.
    if (userCouponId != null) {
      userCouponService.redeem(userId, userCouponId, saved.getOrderId(), itemSubtotal, couponDiscount);
    }

    // 최종 0원 주문: PG 승인 없이 결제 완료 처리. 재고 차감(clearAutomatically로 영속성 컨텍스트가 비워짐)은
    // 결제 행 생성/PAID 전환/응답 조립이 끝난 뒤 마지막에 수행한다.
    boolean zeroAmount = saved.getFinalPrice().signum() == 0;
    if (zeroAmount) {
      paymentCommandService.createZeroAmountPayment(saved.getOrderId());
      saved.markPaid();
    }

    OrderCreateResponseDto response = orderMapper.toCreateResponseDto(saved);

    if (zeroAmount) {
      for (OrderItem oi : orderItems) {
        inventoryService.decreaseStock(oi.getItemVariant().getItemVariantId(), oi.getQuantity());
      }
    }

    OrderCreateRequestDto.OrderSource source = request.getSource();
    if (source == OrderCreateRequestDto.OrderSource.CART_ALL
        || source == OrderCreateRequestDto.OrderSource.CART_SELECTED) {
      List<Long> orderedVariantIds = orderItems.stream()
          .map(oi -> oi.getItemVariant().getItemVariantId())
          .toList();
      removeCartItemsAfterCommit(userId, orderedVariantIds);
    }

    return response;
  }

  /** 내 주문 목록 조회 */
  @Transactional(readOnly = true)
  public List<OrderListItemResponseDto> getMyOrders(Long userId) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    List<Order> orders = orderRepository.findWithItemsByUser_UserIdOrderByOrderIdDesc(userId);

    List<OrderListItemResponseDto> result = new ArrayList<>();
    for (Order o : orders) {
      result.add(new OrderListItemResponseDto(
          o.getOrderId(),
          o.getStatus().name(),
          o.getFinalPrice(),
          o.getCreatedAt(),
          buildOrderSummary(o)
      ));
    }
    return result;
  }

  /** 내 주문 상세 조회 */
  @Transactional(readOnly = true)
  public OrderResponseDto getMyOrder(Long userId, Long orderId) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    Order order = orderRepository.findByOrderIdAndUser_UserId(orderId, userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

    List<OrderItemResponseDto> items = new ArrayList<>();
    for (OrderItem oi : order.getOrderItems()) {
      items.add(orderMapper.toOrderItemResponseDto(oi));
    }
    return orderMapper.toOrderResponseDto(order, items);
  }

  /** 주문 상품 취소 요청 (ORDERED → CANCEL_REQUESTED) */
  public OrderItemResponseDto requestCancelOrderItem(
      Long userId,
      Long orderItemId,
      OrderItemCancelRequestDto request
  ) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    OrderItem oi = orderItemRepository.findByOrderItemIdAndOrder_User_UserId(orderItemId, userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_FOUND));

    if (oi.getStatus() == OrderItemStatus.CANCELED
        || oi.getStatus() == OrderItemStatus.REFUNDED) {
      throw new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_CANCELABLE);
    }

    OrderItemStatus prev = oi.getStatus();
    oi.changeStatus(OrderItemStatus.CANCEL_REQUESTED);

    OrderItemHistory history = OrderItemHistory.create(oi, prev, oi.getStatus(), OrderChangedBy.GENERAL);
    orderItemHistoryRepository.save(history);

    return orderMapper.toOrderItemResponseDto(oi);
  }

  /** 주문 상품 취소 확정 (CANCEL_REQUESTED → CANCELED + 재고 복원) */
  public OrderItemResponseDto confirmCancelOrderItem(Long userId, Long orderItemId) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    OrderItem oi = orderItemRepository.findByOrderItemIdAndOrder_User_UserId(orderItemId, userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_FOUND));

    if (oi.getStatus() != OrderItemStatus.CANCEL_REQUESTED) {
      throw new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_CANCELABLE);
    }

    Long itemVariantId = oi.getItemVariant().getItemVariantId();
    int quantity = oi.getQuantity();

    OrderItemStatus prev = oi.getStatus();
    oi.changeStatus(OrderItemStatus.CANCELED);

    Order order = oi.getOrder();
    order.recalculateAmounts();

    // CREATED(결제 전)에서 시작해 부분취소가 여러 번 거쳐도(PARTIALLY_CANCELED) 계속 전환 대상이어야 함
    if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PARTIALLY_CANCELED) {
      boolean allCanceled = order.getOrderItems().stream()
          .allMatch(item -> item.getStatus() == OrderItemStatus.CANCELED);
      order.changeStatus(allCanceled ? OrderStatus.CANCELED : OrderStatus.PARTIALLY_CANCELED);
    }

    OrderItemHistory history = OrderItemHistory.create(oi, prev, oi.getStatus(), OrderChangedBy.GENERAL);
    orderItemHistoryRepository.save(history);

    OrderItemResponseDto response = orderMapper.toOrderItemResponseDto(oi);

    // increaseStock()은 내부적으로 @Modifying(clearAutomatically=true)라 영속성 컨텍스트를 비운다.
    // 위에서 Order/OrderItem 엔티티 작업과 응답 DTO 계산을 모두 마친 뒤 가장 마지막에 호출해야
    // 이후 지연 로딩(LazyInitializationException) 없이 안전하다.
    inventoryService.increaseStock(itemVariantId, quantity);

    return response;
  }

  // -------------------------
  // private helpers
  // -------------------------

  private List<OrderItem> buildOrderItemsFromCartAll(Long userId) {
    List<CartLineItem> lineItems = cartService.getCartLineItems(userId);
    if (lineItems.isEmpty()) {
      throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);
    }

    List<OrderItem> result = new ArrayList<>();
    for (CartLineItem li : lineItems) {
      ItemVariant variant = li.variant();
      int qty = li.quantity();
      result.add(OrderItem.of(variant, qty, variant.getPrice(),
          snapshotProductName(variant), snapshotOptionSummary(variant)));
    }
    return result;
  }

  private List<OrderItem> buildOrderItemsFromCartSelected(Long userId, List<Long> itemVariantIds) {
    if (itemVariantIds == null || itemVariantIds.isEmpty()) {
      throw new BusinessException(OrderErrorCode.INVALID_CART_ITEM_IDS);
    }

    List<CartLineItem> lineItems = cartService.getCartLineItems(userId, itemVariantIds);
    if (lineItems.isEmpty()) {
      throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);
    }

    List<OrderItem> result = new ArrayList<>();
    for (CartLineItem li : lineItems) {
      ItemVariant variant = li.variant();
      int qty = li.quantity();
      result.add(OrderItem.of(variant, qty, variant.getPrice(),
          snapshotProductName(variant), snapshotOptionSummary(variant)));
    }
    return result;
  }

  /**
   * 주문 DB 커밋이 성공한 뒤에만 주문에 포함된 상품을 장바구니(Redis)에서 제거한다.
   * - Redis 삭제는 DB 트랜잭션에 참여하지 않으므로, 커밋 전에 지우면 주문 롤백 시 장바구니만 유실된다.
   * - CART_ALL도 전체 삭제(clearCart) 대신 주문한 itemVariantId만 지워, 조회 이후 새로 담긴 상품은 남긴다.
   * - 커밋 이후 삭제 실패는 주문 실패가 아니므로 예외를 전파하지 않고 로그만 남긴다.
   */
  private void removeCartItemsAfterCommit(Long userId, List<Long> itemVariantIds) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          cartService.removeItems(userId, itemVariantIds);
        } catch (Exception e) {
          log.error("[createOrder] cart cleanup after commit failed. userId={} itemVariantIds={}",
              userId, itemVariantIds, e);
        }
      }
    });
  }

  private List<OrderItem> buildOrderItemsFromDirect(List<OrderItemCreateRequestDto> items) {
    if (items == null || items.isEmpty()) {
      throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);
    }

    orderValidator.validateDirectItems(items);

    List<OrderItem> result = new ArrayList<>();
    for (OrderItemCreateRequestDto dto : items) {
      ItemVariant variant = itemVariantRepository.findById(dto.getItemVariantId())
          .orElseThrow(() -> new BusinessException(CatalogErrorCode.ITEM_VARIANT_NOT_FOUND));
      result.add(OrderItem.of(variant, dto.getQuantity(), variant.getPrice(),
          snapshotProductName(variant), snapshotOptionSummary(variant)));
    }
    return result;
  }

  private String snapshotProductName(ItemVariant variant) {
    if (variant == null || variant.getItem() == null) return "";
    return variant.getItem().getName();
  }

  private String snapshotOptionSummary(ItemVariant variant) {
    if (variant == null || variant.getItemVariantOptions() == null
        || variant.getItemVariantOptions().isEmpty()) return "";
    return variant.getItemVariantOptions().stream()
        .map(ivo -> ivo.getOption().getValue())
        .collect(Collectors.joining("/"));
  }

  private String buildOrderSummary(Order order) {
    if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) return "";

    OrderItem first = order.getOrderItems().get(0);
    String firstName = "상품";
    ItemVariant v = first.getItemVariant();
    if (v != null && v.getItem() != null && v.getItem().getName() != null) {
      firstName = v.getItem().getName();
    }

    String optionText = buildOptionSummary(v);
    String head = optionText.isBlank() ? firstName : firstName + " (" + optionText + ")";

    int count = order.getOrderItems().size();
    if (count <= 1) return head;
    return head + " 외 " + (count - 1) + "건";
  }

  private String buildOptionSummary(ItemVariant v) {
    if (v == null || v.getItemVariantOptions() == null || v.getItemVariantOptions().isEmpty()) {
      return "";
    }

    String size = null;
    String color = null;

    for (ItemVariantOption ivo : v.getItemVariantOptions()) {
      if (ivo == null) continue;
      Option opt = ivo.getOption();
      if (opt == null || opt.getType() == null || opt.getValue() == null) continue;
      OptionType type = opt.getType();
      switch (type) {
        case SIZE -> size = opt.getValue();
        case COLOR -> color = opt.getValue();
      }
    }

    StringBuilder sb = new StringBuilder();
    if (color != null && !color.isBlank()) sb.append(color.trim());
    if (size != null && !size.isBlank()) {
      if (sb.length() > 0) sb.append("/");
      sb.append(size.trim());
    }
    return sb.toString();
  }
}
