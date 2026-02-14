package com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 확정(confirm) 요청 DTO - successUrl에서 서버로 호출")
public class PaymentConfirmRequestDto {

  @Schema(description = "Payment PK(선택). 있으면 이걸로 조회, 없으면 orderId로 조회", example = "10")
  private Long paymentId;

  @Schema(description = "주문 ID(토스 결제요청에 사용된 orderId)", example = "1001")
  private Long orderId;

  @Schema(description = "PG 결제 키(paymentKey). 토스 successUrl query로 전달됨", example = "pay_1234567890")
  private String paymentKey;

  @Schema(description = "결제 금액. 토스 successUrl query로 전달됨", example = "15000")
  private BigDecimal amount;
}
