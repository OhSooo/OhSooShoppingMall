package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자에게 발급된 쿠폰(발급 단위) 엔티티.
 * - 사용자당 동일 쿠폰 중복 발급을 막기 위해 (user_id, coupon_id)에 유니크 제약을 둔다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_coupon", columnNames = {"user_id", "coupon_id"})
    }
)
public class UserCoupon {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_coupon_id")
  private Long userCouponId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id", nullable = false)
  private Coupon coupon;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UserCouponStatus status;

  @Column(name = "issued_at", nullable = false)
  private OffsetDateTime issuedAt;

  @Column(name = "used_at")
  private OffsetDateTime usedAt;

  @Column(name = "used_order_amount", precision = 19, scale = 2)
  private BigDecimal usedOrderAmount;

  @Column(name = "discount_applied_amount", precision = 19, scale = 2)
  private BigDecimal discountAppliedAmount;

  /** 쿠폰이 사용된 주문 ID (Order 엔티티와의 JPA 연관은 두지 않고 식별자만 보관) */
  @Column(name = "used_order_id")
  private Long usedOrderId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  private UserCoupon(User user, Coupon coupon, OffsetDateTime issuedAt) {
    this.user = user;
    this.coupon = coupon;
    this.status = UserCouponStatus.AVAILABLE;
    this.issuedAt = issuedAt;
  }

  public static UserCoupon issue(User user, Coupon coupon, OffsetDateTime issuedAt) {
    return new UserCoupon(user, coupon, issuedAt);
  }

  public boolean isOwnedBy(Long userId) {
    return this.user != null
        && this.user.getUserId() != null
        && this.user.getUserId().equals(userId);
  }

  /**
   * 영속된 status와 별개로, 현재 시점 기준 실질 상태를 계산한다.
   * - USED는 그대로 USED
   * - AVAILABLE인데 쿠폰 사용 종료일이 지났다면 EXPIRED (별도 배치/스케줄러 없이 lazy하게 판단)
   */
  public UserCouponStatus effectiveStatus(OffsetDateTime now) {
    if (status == UserCouponStatus.USED) {
      return UserCouponStatus.USED;
    }
    if (now.isAfter(coupon.getUseEndAt())) {
      return UserCouponStatus.EXPIRED;
    }
    return UserCouponStatus.AVAILABLE;
  }

  /**
   * AVAILABLE → USED 전환. 이미 USED인 쿠폰을 다시 사용 처리하면 이전 사용 이력을 덮어쓰게 되므로 막는다.
   * 사용 기간/최소 주문금액 등 정책 검증은 서비스(UserCouponService)에서 수행한다.
   */
  public void markUsed(BigDecimal orderAmount, BigDecimal discountAmount, Long orderId, OffsetDateTime usedAt) {
    if (this.status != UserCouponStatus.AVAILABLE) {
      throw new BusinessException(CouponErrorCode.USER_COUPON_ALREADY_USED);
    }
    this.status = UserCouponStatus.USED;
    this.usedOrderAmount = orderAmount;
    this.discountAppliedAmount = discountAmount;
    this.usedOrderId = orderId;
    this.usedAt = usedAt;
  }

  /**
   * USED → AVAILABLE 복구(주문 취소/환불 시). 사용 이력 필드를 초기화한다.
   * - 쿠폰의 사용 기간은 연장하지 않는다. 기간이 이미 지났다면 effectiveStatus()가 EXPIRED로 판단하므로 다시 사용할 수 없다.
   * - USED가 아닌 쿠폰의 복구는 잘못된 상태 전환으로 보고 거부한다.
   */
  public void restore() {
    if (this.status != UserCouponStatus.USED) {
      throw new BusinessException(CouponErrorCode.USER_COUPON_NOT_RESTORABLE);
    }
    this.status = UserCouponStatus.AVAILABLE;
    this.usedOrderAmount = null;
    this.discountAppliedAmount = null;
    this.usedOrderId = null;
    this.usedAt = null;
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
