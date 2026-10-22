package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request.CouponCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.mapper.CouponMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.CouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CouponService {

  private final CouponRepository couponRepository;
  private final CouponMapper couponMapper;

  public CouponResponseDto createCoupon(CouponCreateRequestDto request) {
    Coupon coupon = Coupon.create(
        request.getName(),
        request.getDiscountType(),
        request.getDiscountValue(),
        request.getMinOrderAmount(),
        request.getMaxDiscountAmount(),
        request.getIssueStartAt(),
        request.getIssueEndAt(),
        request.getUseStartAt(),
        request.getUseEndAt()
    );
    couponRepository.save(coupon);
    return couponMapper.toCouponResponseDto(coupon);
  }

  @Transactional(readOnly = true)
  public Coupon getCouponEntity(Long couponId) {
    return couponRepository.findById(couponId)
        .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public CouponResponseDto getCoupon(Long couponId) {
    return couponMapper.toCouponResponseDto(getCouponEntity(couponId));
  }

  @Transactional(readOnly = true)
  public List<CouponResponseDto> getIssuableCoupons() {
    OffsetDateTime now = OffsetDateTime.now();
    return couponRepository
        .findByIssueStartAtLessThanEqualAndIssueEndAtGreaterThanEqual(now, now)
        .stream()
        .map(couponMapper::toCouponResponseDto)
        .toList();
  }
}
