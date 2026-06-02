package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum InventoryErrorCode implements BaseErrorCode {

  // 조회/식별
  ITEM_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "INVENTORY_4041", "상품 판매 단위를 찾을 수 없습니다."),

  // 재고/판매 가능 여부
  INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INVENTORY_4091", "재고가 부족합니다."),
  OUT_OF_STOCK(HttpStatus.CONFLICT, "INVENTORY_4092", "품절된 상품입니다."),
  VARIANT_DISABLED(HttpStatus.FORBIDDEN, "INVENTORY_4031", "판매 중지된 상품입니다."),

  // 요청 검증
  INVALID_STOCK_AMOUNT(HttpStatus.BAD_REQUEST, "INVENTORY_4001", "유효하지 않은 재고 수량입니다."),

  // 권한/범위
  STOCK_ADJUST_FORBIDDEN(HttpStatus.FORBIDDEN, "INVENTORY_4032", "재고를 변경할 권한이 없습니다."),

  // 판매 불가(선택: Item 상태까지 함께 체크할 때)
  ITEM_NOT_SALEABLE(HttpStatus.FORBIDDEN, "INVENTORY_4033", "판매 불가 상태의 상품입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
