package com.ohsooo.platform.ohsooshoppingmall.domain.payment.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.PaymentConfirmRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.PaymentCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentConfirmResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentCreateResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.PaymentResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.service.PaymentCommandService;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.service.PaymentQueryService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments", description = "결제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

  private final PaymentCommandService paymentCommandService;
  private final PaymentQueryService paymentQueryService;

  @Operation(
      summary = "결제 생성(READY)",
      description = """
          주문에 대한 결제 시도를 생성합니다.
          
          - 결제 버튼 클릭 시점에 Payment(READY)를 만들어두고
          - 클라이언트는 이후 confirm(확정) 요청으로 서버가 PG 승인 API를 호출하도록 합니다.
          """
  )
  @PostMapping
  public ResponseEntity<BaseResponse<PaymentCreateResponseDto>> createPayment(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody PaymentCreateRequestDto request
  ) {
    PaymentCreateResponseDto response = paymentCommandService.createPayment(userId, request);
    return ResponseEntity.ok(BaseResponse.success("결제 생성 성공", response));
  }

  @Operation(
      summary = "결제 확정(confirm)",
      description = """
          결제 완료(successUrl) 이후 클라이언트가 서버로 confirm 요청을 보내면,
          서버가 PG 승인(확정) API를 호출하고 Payment 상태를 CAPTURED/FAILED로 갱신합니다.
          """
  )
  @PostMapping("/{paymentId}/confirm")
  public ResponseEntity<BaseResponse<PaymentConfirmResponseDto>> confirmPayment(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long paymentId,
      @Valid @RequestBody PaymentConfirmRequestDto request
  ) {
    PaymentConfirmResponseDto response = paymentCommandService.confirmPayment(userId, paymentId, request);
    return ResponseEntity.ok(BaseResponse.success("결제 확정 성공", response));
  }

  @Operation(
      summary = "내 결제 단건 조회",
      description = "로그인한 사용자가 본인 주문에 대한 결제 정보를 조회합니다."
  )
  @GetMapping("/{paymentId}")
  public ResponseEntity<BaseResponse<PaymentResponseDto>> getMyPayment(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long paymentId
  ) {
    PaymentResponseDto response = paymentQueryService.getMyPayment(userId, paymentId);
    return ResponseEntity.ok(BaseResponse.success("결제 조회 성공", response));
  }

  @Operation(
      summary = "주문 기준 결제 조회",
      description = "주문 ID로 결제를 조회합니다. (현재는 1개 결제 시도 기준으로 단건 반환)"
  )
  @GetMapping("/orders/{orderId}")
  public ResponseEntity<BaseResponse<PaymentResponseDto>> getMyPaymentByOrder(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long orderId
  ) {
    PaymentResponseDto response = paymentQueryService.getMyPaymentByOrderId(userId, orderId);
    return ResponseEntity.ok(BaseResponse.success("주문 결제 조회 성공", response));
  }
}
