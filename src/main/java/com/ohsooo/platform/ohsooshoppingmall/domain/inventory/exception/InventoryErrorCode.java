package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum InventoryErrorCode implements BaseErrorCode {

  ITEM_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "INVENTORY_4041", "상품 판매 단위를 찾을 수 없습니다."),
  INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INVENTORY_4091", "재고가 부족합니다."),
  VARIANT_DISABLED(HttpStatus.FORBIDDEN, "INVENTORY_4031", "판매 중지된 상품입니다."),
  OUT_OF_STOCK(HttpStatus.CONFLICT, "INVENTORY_4092", "품절된 상품입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
