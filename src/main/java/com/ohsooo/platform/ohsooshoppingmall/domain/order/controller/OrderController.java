package com.ohsooo.platform.ohsooshoppingmall.domain.order.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderItemCancelRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderCreateResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderItemResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderListItemResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.service.OrderService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders", description = "주문 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

  private final OrderService orderService;

  /**
   * 주문 생성
   * - CART_ALL: 장바구니 전체 주문
   * - CART_SELECTED: 장바구니 선택 주문(itemVariantIds)
   * - DIRECT: 바로 주문(items)
   */
  @Operation(
      summary = "주문 생성",
      description = """
          주문을 생성합니다.

          - CART_ALL: 내 장바구니 전체를 주문합니다.
          - CART_SELECTED: itemVariantIds에 담긴 장바구니 상품만 선택 주문합니다.
          - DIRECT: items(variantId/quantity) 기반으로 즉시 주문합니다. (장바구니 미경유)

          주문 생성 시점에 priceAtPurchase(구매 당시 가격) / totalPrice를 확정 저장하는 것을 권장합니다.
          """
  )
  @PostMapping
  public ResponseEntity<BaseResponse<OrderCreateResponseDto>> createOrder(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody OrderCreateRequestDto request
  ) {
    OrderCreateResponseDto response = orderService.createOrder(userId, request);
    return ResponseEntity.ok(BaseResponse.success("주문 생성 성공", response));
  }

  @Operation(
      summary = "내 주문 목록 조회",
      description = "로그인한 사용자의 주문 목록을 조회합니다. (기본 최신순)"
  )
  @GetMapping
  public ResponseEntity<BaseResponse<List<OrderListItemResponseDto>>> getMyOrders(
      @AuthenticationPrincipal Long userId
  ) {
    List<OrderListItemResponseDto> response = orderService.getMyOrders(userId);
    return ResponseEntity.ok(BaseResponse.success("주문 목록 조회 성공", response));
  }

  @Operation(
      summary = "내 주문 상세 조회",
      description = "주문 단건 상세를 조회합니다. (주문 상품/상태 포함)"
  )
  @GetMapping("/{orderId}")
  public ResponseEntity<BaseResponse<OrderResponseDto>> getMyOrder(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long orderId
  ) {
    OrderResponseDto response = orderService.getMyOrder(userId, orderId);
    return ResponseEntity.ok(BaseResponse.success("주문 상세 조회 성공", response));
  }

  @Operation(
      summary = "주문 상품 취소 요청",
      description = "주문 상품(OrderItem) 단위로 취소를 요청합니다. (부분 취소/환불로 확장 가능)"
  )
  @PostMapping("/items/{orderItemId}/cancel")
  public ResponseEntity<BaseResponse<OrderItemResponseDto>> cancelOrderItem(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long orderItemId,
      @Valid @RequestBody OrderItemCancelRequestDto request
  ) {
    OrderItemResponseDto response = orderService.requestCancelOrderItem(userId, orderItemId, request);
    return ResponseEntity.ok(BaseResponse.success("주문 상품 취소 요청 완료", response));
  }

  @Operation(
      summary = "주문 상품 취소 확정",
      description = "CANCEL_REQUESTED 상태인 주문 상품을 CANCELED로 확정하고 재고를 복원합니다."
  )
  @PostMapping("/items/{orderItemId}/cancel/confirm")
  public ResponseEntity<BaseResponse<OrderItemResponseDto>> confirmCancelOrderItem(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long orderItemId
  ) {
    OrderItemResponseDto response = orderService.confirmCancelOrderItem(userId, orderItemId);
    return ResponseEntity.ok(BaseResponse.success("주문 상품 취소 확정 완료", response));
  }
}
