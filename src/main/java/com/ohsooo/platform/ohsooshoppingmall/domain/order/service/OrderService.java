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
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.exception.InventoryErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
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

  private final OrderMapper orderMapper;
  private final OrderValidator orderValidator;

  /** 주문 생성 */
  public OrderCreateResponseDto createOrder(Long userId, OrderCreateRequestDto request) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    orderValidator.validateCreateRequest(request);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    // 배송 필수값 체크(요청 or User fallback)
    orderValidator.validateShippingRequiredFields(user, request);

    // 주문상품 구성
    List<OrderItem> orderItems = switch (request.getSource()) {
      case CART_ALL -> buildOrderItemsFromCartAll(userId);
      case CART_SELECTED -> buildOrderItemsFromCartSelected(userId, request.getCartItemIds());
      case DIRECT -> buildOrderItemsFromDirect(request.getItems());
    };

    if (orderItems.isEmpty()) throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEMS);

    // Order 생성 (배송정보 포함)
    Order order = orderMapper.toOrderEntity(user, request);

    for (OrderItem oi : orderItems) {
      order.addOrderItem(oi);
    }

    order.recalculateTotalPrice();

    Order saved = orderRepository.save(order);

    return orderMapper.toCreateResponseDto(saved);
  }

  /** 내 주문 목록 조회 */
  @Transactional(readOnly = true)
  public List<OrderListItemResponseDto> getMyOrders(Long userId) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    // summary 만들기 위해 orderItems + item + options 까지 로딩되어야 함
    List<Order> orders = orderRepository.findWithItemsByUser_UserIdOrderByOrderIdDesc(userId);

    List<OrderListItemResponseDto> result = new ArrayList<>();
    for (Order o : orders) {
      String summary = buildOrderSummary(o);

      result.add(new OrderListItemResponseDto(
          o.getOrderId(),
          o.getStatus().name(),
          o.getTotalPrice(),
          o.getCreatedAt(),
          summary
      ));
    }
    return result;
  }

  /**
   * summary 예시:
   * - 1개면: "오프화이트 티셔츠 (BLACK/M)"
   * - 여러개면: "오프화이트 티셔츠 (BLACK/M) 외 2건"
   * - 옵션 없으면: "오프화이트 티셔츠 외 2건"
   */
  private String buildOrderSummary(Order order) {
    if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) return "";

    OrderItem first = order.getOrderItems().get(0);

    String firstName = "상품";
    ItemVariant v = first.getItemVariant();
    if (v != null && v.getItem() != null && v.getItem().getName() != null) {
      firstName = v.getItem().getName();
    }

    String optionText = buildOptionSummary(v); // 예: "BLACK/M" or ""

    String head = optionText.isBlank()
        ? firstName
        : firstName + " (" + optionText + ")";

    int count = order.getOrderItems().size();
    if (count <= 1) return head;

    return head + " 외 " + (count - 1) + "건";
  }

  /**
   * 첫 상품 옵션 요약: "BLACK/M" 형태 (COLOR/SIZE)
   * - 없으면 "" 반환
   */
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

  /** 주문 상품 취소 요청(ORDERED -> CANCEL_REQUESTED) */
  public OrderItemResponseDto requestCancelOrderItem(
      Long userId,
      Long orderItemId,
      OrderItemCancelRequestDto request
  ) {
    if (userId == null) throw new BusinessException(OrderErrorCode.AUTH_PRINCIPAL_MISSING);

    OrderItem oi = orderItemRepository.findByOrderItemIdAndOrder_User_UserId(orderItemId, userId)
        .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_FOUND));

    // 최소 정책: 이미 취소/환불 확정이면 불가
    if (oi.getStatus() == OrderItemStatus.CANCELED
        || oi.getStatus() == OrderItemStatus.REFUNDED) {
      throw new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_CANCELABLE);
    }

    OrderItemStatus prev = oi.getStatus();
    oi.changeStatus(OrderItemStatus.CANCEL_REQUESTED);

    OrderItemHistory history = OrderItemHistory.create(
        oi,
        prev,
        oi.getStatus(),
        OrderChangedBy.GENERAL
    );
    orderItemHistoryRepository.save(history);

    return orderMapper.toOrderItemResponseDto(oi);
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

      decreaseStockOrThrow(variant.getItemVariantId(), qty);

      result.add(OrderItem.of(variant, qty, variant.getPrice()));
    }
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

    List<OrderItem> result = new ArrayList<>();
    for (CartItem ci : cart.getCartItems()) {
      if (!targets.contains(ci.getCartItemId())) continue;

      ItemVariant variant = ci.getItemVariant();
      int qty = ci.getQuantity();

      decreaseStockOrThrow(variant.getItemVariantId(), qty);

      result.add(OrderItem.of(variant, qty, variant.getPrice()));
    }

    if (result.isEmpty()) throw new BusinessException(OrderErrorCode.INVALID_CART_ITEM_IDS);
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

      decreaseStockOrThrow(variant.getItemVariantId(), dto.getQuantity());

      result.add(OrderItem.of(variant, dto.getQuantity(), variant.getPrice()));
    }

    return result;
  }

  private void decreaseStockOrThrow(Long variantId, int amount) {
    if (amount <= 0) throw new BusinessException(OrderErrorCode.INVALID_QUANTITY);

    int updated = itemVariantRepository.decreaseStockIfEnough(variantId, amount);
    if (updated == 0) {
      throw new BusinessException(InventoryErrorCode.INSUFFICIENT_STOCK);
    }
  }
}
