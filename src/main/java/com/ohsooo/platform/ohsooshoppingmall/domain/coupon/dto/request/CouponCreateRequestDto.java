package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateRequestDto {

  @NotBlank
  private String name;

  @NotNull
  private DiscountType discountType;

  @NotNull
  @DecimalMin(value = "0.0", inclusive = false)
  private BigDecimal discountValue;

  /** 최소 주문 금액 (선택) */
  @DecimalMin(value = "0.0", inclusive = true)
  private BigDecimal minOrderAmount;

  /** 최대 할인 금액 (선택, PERCENTAGE에서 주로 사용) */
  @DecimalMin(value = "0.0", inclusive = false)
  private BigDecimal maxDiscountAmount;

  @NotNull
  private OffsetDateTime issueStartAt;

  @NotNull
  private OffsetDateTime issueEndAt;

  @NotNull
  private OffsetDateTime useStartAt;

  @NotNull
  private OffsetDateTime useEndAt;
}
