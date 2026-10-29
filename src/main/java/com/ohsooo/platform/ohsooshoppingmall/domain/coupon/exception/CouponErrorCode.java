package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CouponErrorCode implements BaseErrorCode {

  // 정책/생성
  INVALID_DISCOUNT_POLICY(HttpStatus.BAD_REQUEST, "COUPON_4001", "유효하지 않은 할인 정책입니다."),
  INVALID_COUPON_PERIOD(HttpStatus.BAD_REQUEST, "COUPON_4002", "유효하지 않은 발급/사용 기간입니다."),

  // 조회
  COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_4041", "쿠폰을 찾을 수 없습니다."),
  USER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_4042", "발급된 쿠폰을 찾을 수 없습니다."),

  // 권한
  USER_COUPON_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COUPON_4031", "해당 쿠폰에 대한 권한이 없습니다."),

  // 발급
  COUPON_ISSUE_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "COUPON_4003", "쿠폰 발급 기간이 아닙니다."),
  COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "COUPON_4091", "이미 발급받은 쿠폰입니다."),

  // 사용 검증
  INVALID_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "COUPON_4004", "유효하지 않은 주문 금액입니다."),
  COUPON_MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "COUPON_4005", "최소 주문 금액 조건을 만족하지 않습니다."),
  USER_COUPON_ALREADY_USED(HttpStatus.CONFLICT, "COUPON_4092", "이미 사용된 쿠폰입니다."),
  USER_COUPON_EXPIRED(HttpStatus.CONFLICT, "COUPON_4093", "사용 기간이 만료된 쿠폰입니다."),
  USER_COUPON_NOT_YET_USABLE(HttpStatus.BAD_REQUEST, "COUPON_4006", "아직 사용 가능한 기간이 아닙니다."),

  // 주문 연동(사용/복구)
  INVALID_ORDER_ID(HttpStatus.BAD_REQUEST, "COUPON_4007", "유효하지 않은 주문 ID입니다."),
  COUPON_DISCOUNT_MISMATCH(HttpStatus.CONFLICT, "COUPON_4094", "쿠폰 할인 금액이 변경되었습니다. 다시 시도해주세요."),
  USER_COUPON_NOT_RESTORABLE(HttpStatus.CONFLICT, "COUPON_4095", "사용되지 않은 쿠폰은 복구할 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
