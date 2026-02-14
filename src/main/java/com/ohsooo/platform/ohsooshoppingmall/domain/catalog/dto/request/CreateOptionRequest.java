package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateOptionRequest {

    @Schema(
            description = "옵션이 속한 상품 ID",
            example = "10",
            required = true
    )
    @NotNull(message = "itemId는 필수입니다")
    private Long itemId;

    @Schema(
            description = "옵션 타입",
            example = "COLOR",
            required = true,
            allowableValues = {"COLOR", "SIZE"}
    )
    @NotNull(message = "옵션 타입은 필수입니다")
    private OptionType type;

    @Schema(
            description = "옵션 값",
            example = "RED",
            required = true
    )
    @NotBlank(message = "옵션 값은 비어 있을 수 없습니다")
    @Size(max = 50, message = "옵션 값은 50자 이내여야 합니다")
    private String value;

    protected CreateOptionRequest() {}
}
