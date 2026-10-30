package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request.UserCouponSearchRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponPageResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCouponStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.CouponService;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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

  // ---------- 내 쿠폰 목록 ----------

  private UserCouponPageResponseDto onePageResponse() {
    CouponResponseDto coupon = new CouponResponseDto(
        3L, "신규가입 3천원", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(3000), BigDecimal.valueOf(10000), null,
        OffsetDateTime.parse("2026-10-01T00:00:00Z"), OffsetDateTime.parse("2026-12-31T00:00:00Z"),
        OffsetDateTime.parse("2026-10-01T00:00:00Z"), OffsetDateTime.parse("2026-12-31T00:00:00Z"));
    UserCouponResponseDto userCoupon = new UserCouponResponseDto(
        11L, coupon, UserCouponStatus.AVAILABLE, true,
        OffsetDateTime.parse("2026-10-30T01:00:00Z"), null, null);
    return new UserCouponPageResponseDto(
        List.of(userCoupon), new UserCouponPageResponseDto.PageInfo(0, 20, 1, 1, true));
  }

  @Test
  void 파라미터가_없으면_page_0_size_20_전체상태로_조회한다() throws Exception {
    ArgumentCaptor<UserCouponSearchRequestDto> captor = ArgumentCaptor.forClass(UserCouponSearchRequestDto.class);
    when(userCouponService.getMyCoupons(any(), captor.capture())).thenReturn(onePageResponse());

    mockMvc.perform(get("/coupons/my"))
        .andExpect(status().isOk());

    assertThat(captor.getValue().getPage()).isZero();
    assertThat(captor.getValue().getSize()).isEqualTo(20);
    assertThat(captor.getValue().getStatus()).isNull();
  }

  @Test
  void page_size_status_파라미터가_조회조건으로_전달된다() throws Exception {
    ArgumentCaptor<UserCouponSearchRequestDto> captor = ArgumentCaptor.forClass(UserCouponSearchRequestDto.class);
    when(userCouponService.getMyCoupons(any(), captor.capture())).thenReturn(onePageResponse());

    mockMvc.perform(get("/coupons/my").param("page", "2").param("size", "100").param("status", "EXPIRED"))
        .andExpect(status().isOk());

    assertThat(captor.getValue().getPage()).isEqualTo(2);
    assertThat(captor.getValue().getSize()).isEqualTo(100);
    assertThat(captor.getValue().getStatus()).isEqualTo(UserCouponStatus.EXPIRED);
  }

  @Test
  void 응답은_쿠폰목록과_페이지정보를_분리해_반환한다() throws Exception {
    when(userCouponService.getMyCoupons(any(), any())).thenReturn(onePageResponse());

    mockMvc.perform(get("/coupons/my"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.coupons[0].userCouponId").value(11))
        .andExpect(jsonPath("$.data.coupons[0].status").value("AVAILABLE"))
        .andExpect(jsonPath("$.data.coupons[0].usableNow").value(true))
        .andExpect(jsonPath("$.data.coupons[0].coupon.couponId").value(3))
        .andExpect(jsonPath("$.data.coupons[0].coupon.name").value("신규가입 3천원"))
        .andExpect(jsonPath("$.data.pageInfo.page").value(0))
        .andExpect(jsonPath("$.data.pageInfo.size").value(20))
        .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
        .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1))
        .andExpect(jsonPath("$.data.pageInfo.last").value(true));
  }

  @ParameterizedTest(name = "{0}={1} → 400")
  @CsvSource({
      "page, -1",
      "page, 21474837",
      "size, 0",
      "size, 101",
      "page, abc",
      "size, ''",
      "status, available",
      "status, UNKNOWN"
  })
  void 잘못된_페이지_파라미터는_400_COMMON_400으로_응답하고_조회하지_않는다(String name, String value) throws Exception {
    mockMvc.perform(get("/coupons/my").param(name, value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("COMMON_400"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith(name + ": ")));

    verify(userCouponService, never()).getMyCoupons(any(), any());
  }

  @Test
  void size_범위초과_오류메시지는_제한값을_알려준다() throws Exception {
    mockMvc.perform(get("/coupons/my").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("size: size는 100 이하여야 합니다."));
  }
}
