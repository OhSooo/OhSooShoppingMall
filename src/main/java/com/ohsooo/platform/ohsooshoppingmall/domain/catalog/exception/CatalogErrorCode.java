package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CatalogErrorCode implements BaseErrorCode {

  ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_4041", "상품을 찾을 수 없습니다."),
  CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_4042", "카테고리를 찾을 수 없습니다."),
  OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_4043", "옵션을 찾을 수 없습니다."),
  ITEM_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_4044", "상품 판매 단위를 찾을 수 없습니다."),

  INVALID_OPTION_TYPE(HttpStatus.BAD_REQUEST, "CATALOG_4001", "유효하지 않은 옵션 타입입니다."),
  DUPLICATE_SKU(HttpStatus.CONFLICT, "CATALOG_4091", "이미 존재하는 SKU입니다."),
  DUPLICATE_OPTION_COMBINATION(HttpStatus.CONFLICT, "CATALOG_4092", "동일한 옵션 조합을 가진 판매 단위가 이미 존재합니다."),
  DUPLICATE_OPTION_TYPE(HttpStatus.BAD_REQUEST, "CATALOG_4002", "하나의 판매 단위 안에 동일한 옵션 타입이 중복될 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
