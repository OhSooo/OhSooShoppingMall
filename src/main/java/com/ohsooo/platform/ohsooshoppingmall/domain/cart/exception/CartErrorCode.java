package com.ohsooo.platform.ohsooshoppingmall.domain.cart.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CartErrorCode implements BaseErrorCode {

  // 인증
  AUTH_PRINCIPAL_MISSING(HttpStatus.UNAUTHORIZED, "CART_4011", "인증 정보가 없습니다."),

  // 조회/식별
  CART_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_4041", "장바구니를 찾을 수 없습니다."),
  CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_4042", "장바구니 상품을 찾을 수 없습니다."),
  ITEM_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_4043", "상품 판매 단위를 찾을 수 없습니다."),

  // 요청 검증
  INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "CART_4001", "유효하지 않은 수량입니다."),
  EXCEEDS_STOCK(HttpStatus.BAD_REQUEST, "CART_4002", "재고를 초과하는 수량입니다."),

  // 상품 상태
  OUT_OF_STOCK(HttpStatus.CONFLICT, "CART_4091", "재고가 없는 상품입니다."),
  ITEM_DISABLED(HttpStatus.CONFLICT, "CART_4092", "판매 중지된 상품입니다."),

  // 권한/소유
  CART_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CART_4031", "해당 장바구니에 대한 권한이 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
