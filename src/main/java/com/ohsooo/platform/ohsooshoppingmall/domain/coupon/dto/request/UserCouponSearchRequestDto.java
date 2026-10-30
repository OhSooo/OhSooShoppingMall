package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 내 쿠폰 목록 조회 조건 (쿼리 파라미터 바인딩용).
 * - 숫자가 아닌 page/size, 정의되지 않은 status는 바인딩 오류로 400(COMMON_400) 응답된다.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserCouponSearchRequestDto {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;
  // Spring Data JPA는 offset(page * size)을 int로 넘기므로, 최대 size에서도 int 범위를 넘지 않도록 제한한다.
  // (넘으면 InvalidDataAccessApiUsageException → 500)
  public static final int MAX_PAGE = Integer.MAX_VALUE / MAX_SIZE;

  @Schema(type = "integer", format = "int32", description = "페이지 번호 (0부터 시작)",
      defaultValue = "0", minimum = "0", maximum = "21474836")
  @Min(value = 0, message = "page는 0 이상이어야 합니다.")
  @Max(value = MAX_PAGE, message = "page는 21474836 이하여야 합니다.")
  private int page = 0;

  @Schema(type = "integer", format = "int32", description = "페이지 크기",
      defaultValue = "20", minimum = "1", maximum = "100")
  @Min(value = 1, message = "size는 1 이상이어야 합니다.")
  @Max(value = MAX_SIZE, message = "size는 100 이하여야 합니다.")
  private int size = DEFAULT_SIZE;

  @Schema(implementation = UserCouponStatus.class, description = """
      상태 필터 (생략 시 전체). 조회 시점 기준 상태로 판단한다.
      - AVAILABLE: 미사용 + 사용 종료일이 지나지 않음 (사용 시작 전 쿠폰 포함, usableNow로 구분)
      - USED: 사용 완료
      - EXPIRED: 미사용 + 사용 종료일이 지남""")
  private UserCouponStatus status;
}
