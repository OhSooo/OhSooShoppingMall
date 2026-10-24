package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.CouponService;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Controller → GlobalExceptionHandler 응답 변환만 검증한다. (Spring 컨텍스트/DB/Security 없음)
 */
@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

  @Mock
  private CouponService couponService;

  @Mock
  private UserCouponService userCouponService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new CouponController(couponService, userCouponService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void 중복발급_예외는_409와_COUPON_4091로_응답한다() throws Exception {
    when(userCouponService.issueCoupon(any(), eq(10L)))
        .thenThrow(new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED));

    mockMvc.perform(post("/coupons/10/issue"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("COUPON_4091"))
        .andExpect(jsonPath("$.message").value(CouponErrorCode.COUPON_ALREADY_ISSUED.getMessage()));
  }

  @Test
  void 변환되지_않은_DB_무결성_예외는_기존대로_500으로_응답한다() throws Exception {
    when(userCouponService.issueCoupon(any(), eq(10L)))
        .thenThrow(new DataIntegrityViolationException("fk violation"));

    mockMvc.perform(post("/coupons/10/issue"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("COMMON_500"));
  }
}
