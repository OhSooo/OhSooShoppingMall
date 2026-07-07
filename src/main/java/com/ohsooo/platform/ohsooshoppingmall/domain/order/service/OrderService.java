package com.ohsooo.platform.ohsooshoppingmall.domain.order.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.Cart;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity.CartItem;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.exception.CartErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository.CartRepository;
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
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderItemHistoryRepository orderItemHistoryRepository;

  private final UserRepository userRepository;
  private final CartRepository cartRepository;
  private final ItemVariantRepository itemVariantRepository;

  private final InventoryService inventoryService;
  private final PricingService pricingService;
  private final OrderMapper orderMapper;
  private final OrderValidator orderValidator;

  /** 주문 생성 */
  public OrderCreateResponseDto createOrder(Long userId, OrderCreateRequestDto request) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    orderValidator.validateCreateRequest(request);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    orderValidator.validateShippingRequiredFields(user, request);

    List<OrderItem> orderItems = switch (request.getSource()) {
      case CART_ALL -> buildOrderItemsFromCartAll(userId);
      case CART_SELECTED -> buildOrderItemsFromCartSelected(userId, request.getCartItemIds());
      case DIRECT -> buildOrderItemsFromDirect(request.getItems());
    };

    if (orderItems.isEmpty()) throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);

    List<PricingLineItem> lineItems = orderItems.stream()
        .map(oi -> new PricingLineItem(oi.getItemVariant(), oi.getQuantity()))
        .toList();
    PricingResult pricing = pricingService.calculate(lineItems);

    Order order = orderMapper.toOrderEntity(user, request);
    for (OrderItem oi : orderItems) {
      order.addOrderItem(oi);
    }
    order.applyPricing(pricing);

    Order saved = orderRepository.save(order);
    return orderMapper.toCreateResponseDto(saved);
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
    Cart cart = cartRepository.findWithItemsByUser_UserId(userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

    if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
      throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);
    }

    List<OrderItem> result = new ArrayList<>();
    for (CartItem ci : cart.getCartItems()) {
      ItemVariant variant = ci.getItemVariant();
      int qty = ci.getQuantity();
      result.add(OrderItem.of(variant, qty, variant.getPrice(),
          snapshotProductName(variant), snapshotOptionSummary(variant)));
    }
    cart.clear();
    return result;
  }

  private List<OrderItem> buildOrderItemsFromCartSelected(Long userId, List<Long> cartItemIds) {
    if (cartItemIds == null || cartItemIds.isEmpty()) {
      throw new BusinessException(OrderErrorCode.INVALID_CART_ITEM_IDS);
    }

    Cart cart = cartRepository.findWithItemsByUser_UserId(userId)
        .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

    if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
      throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);
    }

    Set<Long> targets = new HashSet<>(cartItemIds);
    Set<Long> foundIds = new HashSet<>();

    List<OrderItem> result = new ArrayList<>();
    List<Long> variantIdsToRemove = new ArrayList<>();
    for (CartItem ci : cart.getCartItems()) {
      if (!targets.contains(ci.getCartItemId())) continue;
      ItemVariant variant = ci.getItemVariant();
      int qty = ci.getQuantity();
      result.add(OrderItem.of(variant, qty, variant.getPrice(),
          snapshotProductName(variant), snapshotOptionSummary(variant)));
      variantIdsToRemove.add(variant.getItemVariantId());
      foundIds.add(ci.getCartItemId());
    }

    if (!foundIds.containsAll(targets)) {
      throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
    }

    variantIdsToRemove.forEach(cart::removeItemByVariantId);
    return result;
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
