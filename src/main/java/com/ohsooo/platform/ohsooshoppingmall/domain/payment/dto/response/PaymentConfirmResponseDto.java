package com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 확정(confirm) 응답 DTO")
public class PaymentConfirmResponseDto {

  @Schema(description = "Payment PK", example = "10")
  private Long paymentId;

  @Schema(description = "주문 ID", example = "1001")
  private Long orderId;

  @Schema(description = "결제 상태", example = "CAPTURED")
  private String status;

  @Schema(description = "결제 금액", example = "15000")
  private Integer amount;

  @Schema(description = "통화", example = "KRW")
  private String currency;

  @Schema(description = "PG 결제 키(paymentKey)", example = "pay_1234567890")
  private String pgPaymentKey;

  @Schema(description = "PG 트랜잭션 ID", example = "tx_1234567890")
  private String pgTransactionId;

  @Schema(description = "승인 시각", example = "2026-01-29T02:10:00+09:00")
  private OffsetDateTime approvedAt;
}
