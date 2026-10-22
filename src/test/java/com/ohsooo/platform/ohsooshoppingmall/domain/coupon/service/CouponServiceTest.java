package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.mapper.CouponMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.CouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

  @Mock
  private CouponRepository couponRepository;

  private final CouponMapper couponMapper = new CouponMapper();

  private CouponService couponService;

  @Test
  void 존재하지_않는_쿠폰을_조회하면_예외가_발생한다() {
    couponService = new CouponService(couponRepository, couponMapper);
    when(couponRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> couponService.getCouponEntity(999L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.COUPON_NOT_FOUND);
  }

  @Test
  void 존재하지_않는_쿠폰_조회_에러코드는_404이다() {
    couponService = new CouponService(couponRepository, couponMapper);
    when(couponRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> couponService.getCoupon(999L))
        .isInstanceOf(BusinessException.class);
    assertThat(CouponErrorCode.COUPON_NOT_FOUND.getStatus().value()).isEqualTo(404);
  }
}
