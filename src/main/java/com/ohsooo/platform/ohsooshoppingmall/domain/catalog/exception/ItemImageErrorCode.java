package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ItemImageErrorCode implements BaseErrorCode {

    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM_IMAGE_404", "이미지를 찾을 수 없습니다"),
    IMAGE_NOT_BELONG_TO_ITEM(HttpStatus.BAD_REQUEST, "ITEM_IMAGE_400_1", "해당 상품에 속하지 않는 이미지입니다"),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "ITEM_IMAGE_400_2", "이미지는 최대 10개까지 등록할 수 있습니다"),
    ITEM_DELETED(HttpStatus.BAD_REQUEST, "ITEM_IMAGE_400_3", "삭제된 상품에는 이미지를 등록할 수 없습니다"),
    IMAGE_ORDER_EMPTY(HttpStatus.BAD_REQUEST, "ITEM_IMAGE_400_4", "이미지 순서 목록은 비어 있을 수 없습니다"),
    IMAGE_ORDER_DUPLICATE(HttpStatus.BAD_REQUEST, "ITEM_IMAGE_400_5", "이미지 ID에 중복이 있습니다"),
    IMAGE_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "ITEM_IMAGE_400_6", "요청된 이미지 목록이 해당 상품의 전체 이미지와 일치하지 않습니다"),
    PRIMARY_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "ITEM_IMAGE_400_7", "대표 이미지는 최소 1개 필요합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
