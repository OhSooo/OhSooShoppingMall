package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 정책(마스터) 엔티티.
 * - 할인 정책(정액/정률), 최소주문금액, 최대할인금액, 발급기간, 사용기간을 가진다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupons")
public class Coupon {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "coupon_id")
  private Long couponId;

  @Column(nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "discount_type", nullable = false, length = 30)
  private DiscountType discountType;

  @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
  private BigDecimal discountValue;

  @Column(name = "min_order_amount", precision = 19, scale = 2)
  private BigDecimal minOrderAmount;

  @Column(name = "max_discount_amount", precision = 19, scale = 2)
  private BigDecimal maxDiscountAmount;

  @Column(name = "issue_start_at", nullable = false)
  private OffsetDateTime issueStartAt;

  @Column(name = "issue_end_at", nullable = false)
  private OffsetDateTime issueEndAt;

  @Column(name = "use_start_at", nullable = false)
  private OffsetDateTime useStartAt;

  @Column(name = "use_end_at", nullable = false)
  private OffsetDateTime useEndAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  private Coupon(
      String name,
      DiscountType discountType,
      BigDecimal discountValue,
      BigDecimal minOrderAmount,
      BigDecimal maxDiscountAmount,
      OffsetDateTime issueStartAt,
      OffsetDateTime issueEndAt,
      OffsetDateTime useStartAt,
      OffsetDateTime useEndAt
  ) {
    this.name = name;
    this.discountType = discountType;
    this.discountValue = discountValue;
    this.minOrderAmount = minOrderAmount;
    this.maxDiscountAmount = maxDiscountAmount;
    this.issueStartAt = issueStartAt;
    this.issueEndAt = issueEndAt;
    this.useStartAt = useStartAt;
    this.useEndAt = useEndAt;
  }

  public static Coupon create(
      String name,
      DiscountType discountType,
      BigDecimal discountValue,
      BigDecimal minOrderAmount,
      BigDecimal maxDiscountAmount,
      OffsetDateTime issueStartAt,
      OffsetDateTime issueEndAt,
      OffsetDateTime useStartAt,
      OffsetDateTime useEndAt
  ) {
    validatePolicy(discountType, discountValue, minOrderAmount, maxDiscountAmount);
    validatePeriod(issueStartAt, issueEndAt, useStartAt, useEndAt);

    return new Coupon(
        name,
        discountType,
        discountValue,
        minOrderAmount,
        maxDiscountAmount,
        issueStartAt,
        issueEndAt,
        useStartAt,
        useEndAt
    );
  }

  private static void validatePolicy(
      DiscountType discountType,
      BigDecimal discountValue,
      BigDecimal minOrderAmount,
      BigDecimal maxDiscountAmount
  ) {
    if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(CouponErrorCode.INVALID_DISCOUNT_POLICY);
    }
    if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(HUNDRED) > 0) {
      throw new BusinessException(CouponErrorCode.INVALID_DISCOUNT_POLICY);
    }
    if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(CouponErrorCode.INVALID_DISCOUNT_POLICY);
    }
    if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(CouponErrorCode.INVALID_DISCOUNT_POLICY);
    }
    // 금액 값은 원 단위(정수)만 허용한다. 정률(%) 값은 금액이 아니므로 대상이 아니다.
    if ((discountType == DiscountType.FIXED_AMOUNT && !isWholeWon(discountValue))
        || (minOrderAmount != null && !isWholeWon(minOrderAmount))
        || (maxDiscountAmount != null && !isWholeWon(maxDiscountAmount))) {
      throw new BusinessException(CouponErrorCode.INVALID_DISCOUNT_POLICY);
    }
  }

  /** 원 미만(소수점) 금액이 없는지 확인한다. 예) 1000, 1000.00 → true / 1000.50 → false */
  public static boolean isWholeWon(BigDecimal amount) {
    return amount.stripTrailingZeros().scale() <= 0;
  }

  private static void validatePeriod(
      OffsetDateTime issueStartAt,
      OffsetDateTime issueEndAt,
      OffsetDateTime useStartAt,
      OffsetDateTime useEndAt
  ) {
    if (issueStartAt == null || issueEndAt == null || useStartAt == null || useEndAt == null) {
      throw new BusinessException(CouponErrorCode.INVALID_COUPON_PERIOD);
    }
    if (!issueStartAt.isBefore(issueEndAt) || !useStartAt.isBefore(useEndAt)) {
      throw new BusinessException(CouponErrorCode.INVALID_COUPON_PERIOD);
    }
  }

  public boolean isIssuableAt(OffsetDateTime now) {
    return !now.isBefore(issueStartAt) && !now.isAfter(issueEndAt);
  }

  public boolean isUsableAt(OffsetDateTime now) {
    return !now.isBefore(useStartAt) && !now.isAfter(useEndAt);
  }

  public boolean meetsMinOrderAmount(BigDecimal orderAmount) {
    return minOrderAmount == null || orderAmount.compareTo(minOrderAmount) >= 0;
  }

  /**
   * 주문 금액 기준 할인 금액을 원 단위로 계산한다.
   * - orderAmount: 배송비를 제외한 상품 금액 합계
   * - FIXED_AMOUNT: discountValue와 주문금액 중 작은 값
   * - PERCENTAGE: 주문금액 * discountValue%를 원 미만 절사 후 maxDiscountAmount로 캡, 주문금액을 넘지 않도록 캡
   * - 할인액이 주문금액과 같아 최종 금액이 0원이 되는 것은 허용한다.
   */
  public BigDecimal calculateDiscount(BigDecimal orderAmount) {
    BigDecimal discount;
    if (discountType == DiscountType.FIXED_AMOUNT) {
      discount = discountValue;
    } else {
      discount = orderAmount
          .multiply(discountValue)
          .divide(HUNDRED, 0, RoundingMode.DOWN);
      if (maxDiscountAmount != null) {
        discount = discount.min(maxDiscountAmount);
      }
    }
    return discount.min(orderAmount);
  }

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }
}
