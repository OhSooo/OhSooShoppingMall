package com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentMethod;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 준비(READY Payment 생성) 요청 DTO")
public class PaymentCreateRequestDto {

  @Schema(description = "주문 ID", example = "1001")
  private Long orderId;

  @Schema(description = "결제 금액(서버에서 주문 금액과 검증 권장)", example = "15000")
  private BigDecimal amount;

  @Schema(description = "통화", example = "KRW")
  private String currency;

  @Schema(description = "결제 수단", example = "CARD")
  private PaymentMethod method;

  @Schema(description = "PG 제공자", example = "TOSS")
  private PaymentProvider provider;

  @Schema(description = "주문명(결제창 표시용, 선택)", example = "오쑤 쇼핑몰 주문 1001")
  private String orderName;

  @Schema(description = "구매자명(결제창 표시용, 선택)", example = "Soojin")
  private String customerName;
}
