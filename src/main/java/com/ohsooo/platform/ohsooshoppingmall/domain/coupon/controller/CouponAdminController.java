package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request.CouponCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.CouponService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Coupon", description = "관리자용 쿠폰 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/coupons")
public class CouponAdminController {

  private final CouponService couponService;

  @Operation(summary = "쿠폰 생성", description = "관리자가 새 쿠폰 정책을 생성합니다.")
  @PostMapping
  public ResponseEntity<BaseResponse<CouponResponseDto>> createCoupon(
      @Valid @RequestBody CouponCreateRequestDto request
  ) {
    CouponResponseDto response = couponService.createCoupon(request);
    return ResponseEntity.ok(BaseResponse.success("쿠폰 생성 성공", response));
  }
}
