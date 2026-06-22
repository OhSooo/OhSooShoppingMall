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
    STORE_BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_BANNER_404", "스토어 배너를 찾을 수 없습니다"),
    STORE_BANNER_NOT_BELONG_TO_STORE(HttpStatus.BAD_REQUEST, "STORE_BANNER_400", "해당 배너는 이 스토어에 속하지 않습니다"),
    STORE_BANNER_ORDER_DUPLICATED(HttpStatus.BAD_REQUEST, "STORE_BANNER_400_DUP", "배너 ID 목록에 중복이 있습니다"),
    STORE_BANNER_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "STORE_BANNER_400_MISMATCH", "요청한 배너 ID와 실제 활성 배너가 일치하지 않습니다"),
    STORE_BANNER_INACTIVE_CANNOT_REORDER(HttpStatus.BAD_REQUEST, "STORE_BANNER_400_INACTIVE", "비활성 배너는 순서를 변경할 수 없습니다"),
    STORE_DELIVERY_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_DELIVERY_POLICY_404", "스토어 배송정책을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
