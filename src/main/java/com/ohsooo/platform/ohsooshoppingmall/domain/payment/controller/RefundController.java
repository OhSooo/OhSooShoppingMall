package com.ohsooo.platform.ohsooshoppingmall.domain.payment.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.RefundCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.RefundDetailResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.RefundResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.service.RefundCommandService;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.service.PaymentQueryService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Refunds", description = "환불 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class RefundController {

  private final RefundCommandService refundCommandService;
  private final PaymentQueryService paymentQueryService;

  @Operation(
      summary = "환불 생성(요청)",
      description = """
          결제(paymentId) 기준으로 환불을 생성합니다.
          - 부분 환불/전액 환불 모두 지원할 수 있도록 환불 품목(RefundItem)을 함께 받습니다.
          - 지금 단계에서는 '환불 요청 레코드 생성' + '환불 정책 검증' 중심으로 구현합니다.
          """
  )
  @PostMapping("/{paymentId}/refunds")
  public ResponseEntity<BaseResponse<RefundResponseDto>> createRefund(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long paymentId,
      @Valid @RequestBody RefundCreateRequestDto request
  ) {
    RefundResponseDto response = refundCommandService.createRefund(userId, paymentId, request);
    return ResponseEntity.ok(BaseResponse.success("환불 요청 생성 성공", response));
  }

  @Operation(
      summary = "환불 상세 조회",
      description = "환불 단건 상세(환불 품목 포함)를 조회합니다."
  )
  @GetMapping("/refunds/{refundId}")
  public ResponseEntity<BaseResponse<RefundDetailResponseDto>> getRefundDetail(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long refundId
  ) {
    RefundDetailResponseDto response = paymentQueryService.getMyRefundDetail(userId, refundId);
    return ResponseEntity.ok(BaseResponse.success("환불 상세 조회 성공", response));
  }
}
