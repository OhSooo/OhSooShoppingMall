package com.ohsooo.platform.ohsooshoppingmall.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "스토어 생성 요청 DTO")
public class CreateStoreRequest {

    @Schema(description = "스토어 소유자 ID", example = "123")
    private Long ownerId;

    @Schema(description = "스토어 이름", example = "오수 전자상점")
    private String name;

    @Schema(description = "스토어 설명", example = "전자기기 전문 스토어")
    private String description;

    protected CreateStoreRequest() {
    }
}