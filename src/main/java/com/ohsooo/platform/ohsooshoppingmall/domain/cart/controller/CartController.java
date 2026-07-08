package com.ohsooo.platform.ohsooshoppingmall.domain.cart.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartItemAddRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartItemUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.request.CartMergeRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartMergeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response.CartResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.cart.service.CartService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart", description = "장바구니(Cart) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

  private final CartService cartService;

  @Operation(
      summary = "내 장바구니 조회",
      description = "로그인한 사용자의 장바구니와 장바구니 아이템 목록을 조회합니다."
  )
  @GetMapping
  public ResponseEntity<BaseResponse<CartResponseDto>> getMyCart(
      @AuthenticationPrincipal Long userId
  ) {
    CartResponseDto response = cartService.getMyCart(userId);
    return ResponseEntity.ok(BaseResponse.success("장바구니 조회 성공", response));
  }

  @Operation(
      summary = "장바구니 상품 추가",
      description = """
          로그인한 사용자의 장바구니에 상품 판매 단위(itemVariantId)를 추가합니다.
          
          - 동일 itemVariantId가 이미 존재하면 수량을 증가시키는 정책으로 동작할 수 있습니다. (서비스 정책)
          """
  )
  @PostMapping("/items")
  public ResponseEntity<BaseResponse<CartResponseDto>> addItem(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody CartItemAddRequestDto request
  ) {
    CartResponseDto response = cartService.addItem(userId, request);
    return ResponseEntity.ok(BaseResponse.success("장바구니 추가 성공", response));
  }

  @Operation(
      summary = "비로그인 장바구니 병합",
      description = """
          로그인 성공 시 프론트엔드 로컬(비로그인) 장바구니 목록을 서버 장바구니로 병합합니다.

          - 이미 담긴 상품은 수량을 합산합니다.
          - 품절/판매중지/재고초과 상품은 해당 항목만 병합에서 제외되고, 사유와 함께 skippedItems로 안내됩니다.
          """
  )
  @PostMapping("/merge")
  public ResponseEntity<BaseResponse<CartMergeResponseDto>> mergeGuestCart(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody CartMergeRequestDto request
  ) {
    CartMergeResponseDto response = cartService.mergeGuestCart(userId, request);
    return ResponseEntity.ok(BaseResponse.success("장바구니 병합 성공", response));
  }

  @Operation(
      summary = "장바구니 상품 수량 변경",
      description = "itemVariantId 기준으로 장바구니 상품 수량을 변경합니다."
  )
  @PatchMapping("/items/{itemVariantId}")
  public ResponseEntity<BaseResponse<CartResponseDto>> updateItemQuantity(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long itemVariantId,
      @Valid @RequestBody CartItemUpdateRequestDto request
  ) {
    CartResponseDto response = cartService.updateItemQuantity(userId, itemVariantId, request);
    return ResponseEntity.ok(BaseResponse.success("장바구니 수량 변경 성공", response));
  }

  @Operation(
      summary = "장바구니 상품 삭제",
      description = "itemVariantId 기준으로 장바구니 상품을 삭제합니다."
  )
  @DeleteMapping("/items/{itemVariantId}")
  public ResponseEntity<BaseResponse<Void>> removeItem(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long itemVariantId
  ) {
    cartService.removeItem(userId, itemVariantId);
    return ResponseEntity.ok(BaseResponse.success("장바구니 상품 삭제 성공", null));
  }

  @Operation(
      summary = "장바구니 비우기",
      description = "로그인한 사용자의 장바구니를 비웁니다."
  )
  @DeleteMapping("/items")
  public ResponseEntity<BaseResponse<Void>> clearCart(
      @AuthenticationPrincipal Long userId
  ) {
    cartService.clearCart(userId);
    return ResponseEntity.ok(BaseResponse.success("장바구니 비우기 성공", null));
  }
}
