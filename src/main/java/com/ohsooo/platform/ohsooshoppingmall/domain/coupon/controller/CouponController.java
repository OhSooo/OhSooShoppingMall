package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request.CouponOrderAmountRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request.UserCouponSearchRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponDiscountResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponPageResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.CouponService;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Coupons", description = "쿠폰 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
public class CouponController {

  private final CouponService couponService;
  private final UserCouponService userCouponService;

  @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "현재 발급 기간 내에 있는 쿠폰 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<BaseResponse<List<CouponResponseDto>>> getIssuableCoupons() {
    List<CouponResponseDto> response = couponService.getIssuableCoupons();
    return ResponseEntity.ok(BaseResponse.success("발급 가능 쿠폰 목록 조회 성공", response));
  }

  @Operation(summary = "쿠폰 상세 조회", description = "쿠폰 ID로 쿠폰 정책을 조회합니다.")
  @GetMapping("/{couponId}")
  public ResponseEntity<BaseResponse<CouponResponseDto>> getCoupon(@PathVariable Long couponId) {
    CouponResponseDto response = couponService.getCoupon(couponId);
    return ResponseEntity.ok(BaseResponse.success("쿠폰 조회 성공", response));
  }

  @Operation(summary = "쿠폰 발급받기", description = "로그인한 사용자가 본인에게 쿠폰을 발급받습니다. 사용자당 1회만 발급 가능합니다.")
  @PostMapping("/{couponId}/issue")
  public ResponseEntity<BaseResponse<UserCouponResponseDto>> issueCoupon(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long couponId
  ) {
    UserCouponResponseDto response = userCouponService.issueCoupon(userId, couponId);
    return ResponseEntity.ok(BaseResponse.success("쿠폰 발급 성공", response));
  }

  @Operation(
      summary = "내 쿠폰 목록 조회",
      description = """
          로그인한 사용자가 발급받은 쿠폰 목록을 페이지 단위로 조회합니다.

          - 정렬: 발급일시 최신순, 같은 발급일시는 userCouponId 내림차순 (고정)
          - page는 0부터 시작, size 기본 20 / 최대 100
          - status 필터는 조회 시점 기준 상태로 적용되며 totalElements/totalPages도 필터 결과 기준입니다.
          - AVAILABLE에는 사용 시작 전 쿠폰도 포함됩니다. 지금 바로 사용 가능한지는 usableNow로 확인합니다.
          - 범위를 벗어난 page는 빈 coupons와 함께 200으로 응답합니다.
          - page < 0, page > 21474836, size < 1, size > 100, 숫자가 아닌 page/size, 정의되지 않은 status는 400(COMMON_400)입니다.
          """
  )
  @GetMapping("/my")
  public ResponseEntity<BaseResponse<UserCouponPageResponseDto>> getMyCoupons(
      @AuthenticationPrincipal Long userId,
      @Valid @ParameterObject UserCouponSearchRequestDto condition
  ) {
    UserCouponPageResponseDto response = userCouponService.getMyCoupons(userId, condition);
    return ResponseEntity.ok(BaseResponse.success("내 쿠폰 목록 조회 성공", response));
  }

  @Operation(
      summary = "쿠폰 할인 미리보기",
      description = "쿠폰 사용 가능 여부를 검증하고, 주어진 주문 금액 기준 할인 금액을 계산합니다. (상태 변경 없음)"
  )
  @PostMapping("/my/{userCouponId}/preview")
  public ResponseEntity<BaseResponse<CouponDiscountResponseDto>> previewDiscount(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long userCouponId,
      @Valid @RequestBody CouponOrderAmountRequestDto request
  ) {
    CouponDiscountResponseDto response =
        userCouponService.previewDiscount(userId, userCouponId, request.getOrderAmount());
    return ResponseEntity.ok(BaseResponse.success("쿠폰 할인 미리보기 성공", response));
  }

  @Operation(
      summary = "쿠폰 사용 처리",
      description = "쿠폰 사용 가능 여부를 검증하고 할인 금액을 계산한 뒤, 쿠폰을 사용 완료 상태로 처리합니다."
  )
  @PostMapping("/my/{userCouponId}/use")
  public ResponseEntity<BaseResponse<CouponDiscountResponseDto>> useCoupon(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long userCouponId,
      @Valid @RequestBody CouponOrderAmountRequestDto request
  ) {
    CouponDiscountResponseDto response =
        userCouponService.useCoupon(userId, userCouponId, request.getOrderAmount(), request.getOrderId());
    return ResponseEntity.ok(BaseResponse.success("쿠폰 사용 처리 성공", response));
  }
}
