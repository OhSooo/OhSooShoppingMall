package com.ohsooo.platform.ohsooshoppingmall.domain.store.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StoreErrorCode implements BaseErrorCode {

    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_404", "스토어를 찾을 수 없습니다"),
    STORE_OWNER_FORBIDDEN(HttpStatus.FORBIDDEN, "STORE_403", "본인 스토어만 변경할 수 있습니다"),
    OWNER_CANNOT_SUSPEND_STORE(HttpStatus.BAD_REQUEST, "STORE_400", "해당 상태로 변경할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
