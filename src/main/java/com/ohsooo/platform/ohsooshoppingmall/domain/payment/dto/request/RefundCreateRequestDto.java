package com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "환불 생성 요청 DTO")
public class RefundCreateRequestDto {

  @Schema(description = "대상 결제(payment) ID", example = "10")
  private Long paymentId;

  @Schema(description = "환불 총액", example = "5000")
  private Integer amount;

  @Schema(description = "환불 사유(선택)", example = "단순 변심")
  private String reason;

  @Schema(description = "환불 품목 목록(부분환불/품목환불 시 사용, 선택)")
  private List<RefundItemRequestDto> items;
}
