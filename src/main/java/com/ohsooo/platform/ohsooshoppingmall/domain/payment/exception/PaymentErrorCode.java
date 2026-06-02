package com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {

  // 인증
  AUTH_PRINCIPAL_MISSING(HttpStatus.UNAUTHORIZED, "PAYMENT_4011", "인증 정보가 없습니다."),

  // 조회/식별
  PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_4041", "결제 정보를 찾을 수 없습니다."),
  PAYMENT_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_4042", "결제 이벤트를 찾을 수 없습니다."),
  REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_4043", "환불 정보를 찾을 수 없습니다."),

  // 요청 검증
  INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PAYMENT_4001", "유효하지 않은 결제 금액입니다."),
  INVALID_REFUND_AMOUNT(HttpStatus.BAD_REQUEST, "PAYMENT_4002", "유효하지 않은 환불 금액입니다."),
  INVALID_REFUND_ITEM(HttpStatus.BAD_REQUEST, "PAYMENT_4003", "유효하지 않은 환불 상품 정보입니다."),
  INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "PAYMENT_4004", "유효하지 않은 PG 제공자입니다."),
  INVALID_METHOD(HttpStatus.BAD_REQUEST, "PAYMENT_4005", "유효하지 않은 결제 수단입니다."),

  // 상태/정책(결제 상태 전이)
  PAYMENT_NOT_CONFIRMABLE(HttpStatus.CONFLICT, "PAYMENT_4091", "현재 상태에서는 결제 확정이 불가능합니다."),
  PAYMENT_ALREADY_CAPTURED(HttpStatus.CONFLICT, "PAYMENT_4092", "이미 결제 확정된 결제입니다."),
  PAYMENT_ALREADY_FAILED(HttpStatus.CONFLICT, "PAYMENT_4093", "이미 실패 처리된 결제입니다."),
  PAYMENT_NOT_CANCELABLE(HttpStatus.CONFLICT, "PAYMENT_4094", "현재 상태에서는 결제 취소가 불가능합니다."),
  PAYMENT_NOT_REFUNDABLE(HttpStatus.CONFLICT, "PAYMENT_4095", "현재 상태에서는 환불이 불가능합니다."),

  // 환불 정책
  REFUND_ALREADY_PROCESSED(HttpStatus.CONFLICT, "PAYMENT_4096", "이미 처리된 환불 요청입니다."),
  REFUND_ITEM_EXCEEDS_ALLOWABLE(HttpStatus.CONFLICT, "PAYMENT_4097", "환불 가능한 수량 또는 금액을 초과했습니다."),

  // PG/웹훅/통신
  PG_SIGNATURE_INVALID(HttpStatus.FORBIDDEN, "PAYMENT_4031", "PG 웹훅 서명 검증에 실패했습니다."),
  PG_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT_5021", "PG API 호출에 실패했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
