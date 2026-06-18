package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ItemErrorCode implements BaseErrorCode {

    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM_404", "상품을 찾을 수 없습니다"),
    ITEM_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "ITEM_400_1", "이미 삭제된 상품입니다"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "ITEM_400_2", "허용되지 않는 상태 변경입니다"),
    INVALID_ITEM_NAME(HttpStatus.BAD_REQUEST, "ITEM_400_3", "상품명은 빈 값일 수 없습니다");
    
    private final HttpStatus status;
    private final String code;
    private final String message;
}
