package com.ohsooo.platform.ohsooshoppingmall.domain.order.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements BaseErrorCode {

  // 인증
  AUTH_PRINCIPAL_MISSING(HttpStatus.UNAUTHORIZED, "ORDER_4011", "인증 정보가 없습니다."),

  // 조회/식별
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_4041", "주문을 찾을 수 없습니다."),
  ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_4042", "주문 상품을 찾을 수 없습니다."),

  // 권한/소유
  ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORDER_4031", "해당 주문에 대한 권한이 없습니다."),

  // 요청 검증
  INVALID_ORDER_SOURCE(HttpStatus.BAD_REQUEST, "ORDER_4001", "유효하지 않은 주문 생성 방식입니다."),
  EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "ORDER_4002", "주문 상품이 비어있습니다."),
  INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_4003", "유효하지 않은 수량입니다."),
  INVALID_CART_ITEM_IDS(HttpStatus.BAD_REQUEST, "ORDER_4004", "선택한 장바구니 상품이 비어있습니다."),
  SHIPPING_REQUIRED_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "ORDER_4005", "배송 필수 정보가 누락되었습니다."),

  // 상태/정책
  ORDER_ITEM_NOT_CANCELABLE(HttpStatus.CONFLICT, "ORDER_4091", "현재 상태에서는 주문 상품 취소가 불가능합니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
