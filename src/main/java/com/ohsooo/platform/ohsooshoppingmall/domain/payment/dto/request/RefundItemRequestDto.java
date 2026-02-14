package com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "환불 품목 요청 DTO")
public class RefundItemRequestDto {

  @Schema(description = "환불 대상 주문상품 ID", example = "5001")
  private Long orderItemId;

  @Schema(description = "해당 품목에 대한 환불 금액", example = "5000")
  private BigDecimal amount;

  @Schema(description = "환불 수량", example = "1")
  private Integer quantity;
}
