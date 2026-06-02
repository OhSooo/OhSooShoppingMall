package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ItemVariantErrorCode implements BaseErrorCode {

    ITEM_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM_VARIANT_404", "상품 단위를 찾을 수 없습니다"),
    ITEM_VARIANT_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ITEM_VARIANT_400", "재고 수량은 0 이상이어야 합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
