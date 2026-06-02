package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements BaseErrorCode {

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_404", "카테고리를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
