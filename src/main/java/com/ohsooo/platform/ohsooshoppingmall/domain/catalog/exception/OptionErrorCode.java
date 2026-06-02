package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OptionErrorCode implements BaseErrorCode {

    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "OPTION_404", "옵션을 찾을 수 없습니다"),
    OPTION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "OPTION_400", "이미 존재하는 옵션입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
