package com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "환불 요약 응답 DTO")
public class RefundResponseDto {

  @Schema(description = "환불 ID", example = "1")
  private Long refundId;

  @Schema(description = "대상 결제 ID", example = "10")
  private Long paymentId;

  @Schema(description = "환불 상태", example = "REQUESTED")
  private String status;

  @Schema(description = "환불 금액", example = "5000")
  private Integer amount;

  @Schema(description = "환불 사유(선택)", example = "단순 변심")
  private String reason;

  @Schema(description = "환불 요청 생성 시각", example = "2026-01-29T02:20:00+09:00")
  private OffsetDateTime createdAt;

  @Schema(description = "환불 완료 시각(성공 시)", example = "2026-01-29T02:25:00+09:00")
  private OffsetDateTime refundedAt;
}
