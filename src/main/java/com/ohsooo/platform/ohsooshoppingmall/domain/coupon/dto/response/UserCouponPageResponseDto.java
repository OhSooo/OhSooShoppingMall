package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 내 쿠폰 목록 페이지 응답. 쿠폰 목록과 페이지 메타데이터를 분리한다.
 * (Spring Data의 Page를 그대로 직렬화하면 pageable/sort 등 내부 구조가 노출되고 버전에 따라 형태가 바뀔 수 있다.)
 */
@Getter
@AllArgsConstructor
public class UserCouponPageResponseDto {

  @Schema(description = "쿠폰 목록 (발급일시 최신순, 같으면 userCouponId 내림차순)")
  private List<UserCouponResponseDto> coupons;

  @Schema(description = "페이지 정보")
  private PageInfo pageInfo;

  @Getter
  @AllArgsConstructor
  public static class PageInfo {

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 쿠폰 수 (필터 적용 후)", example = "42")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "3")
    private int totalPages;

    @Schema(description = "마지막 페이지 여부 (범위를 벗어난 페이지도 true)", example = "false")
    private boolean last;
  }
}
