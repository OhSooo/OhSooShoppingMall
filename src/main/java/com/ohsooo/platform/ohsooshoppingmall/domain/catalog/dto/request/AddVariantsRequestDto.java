package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;

@Getter
public class AddVariantsRequestDto {

    @Schema(description = "추가할 판매 단위(Variant) 목록", required = true)
    @NotEmpty(message = "variants는 하나 이상 필요합니다")
    @Valid
    private List<VariantDto> variants;

    protected AddVariantsRequestDto() {}

    @Getter
    public static class VariantDto {

        @Schema(description = "SKU (고유 식별자)", example = "TSHIRT-WHITE-L", required = true)
        @NotBlank(message = "sku는 필수입니다")
        @Size(max = 100, message = "sku는 100자 이내여야 합니다")
        private String sku;

        @Schema(description = "판매 가격", example = "22000", required = true)
        @NotNull(message = "price는 필수입니다")
        @DecimalMin(value = "1.0", inclusive = true, message = "price는 1 이상이어야 합니다")
        private BigDecimal price;

        @Schema(description = "초기 재고 수량", example = "8", required = true)
        @NotNull(message = "initialQuantity는 필수입니다")
        @PositiveOrZero(message = "initialQuantity는 0 이상이어야 합니다")
        private Integer initialQuantity;

        @Schema(description = "옵션 목록 (SIZE, COLOR)")
        @Valid
        private List<OptionDto> options;

        protected VariantDto() {}
    }

    @Getter
    public static class OptionDto {

        @Schema(description = "옵션 타입", example = "COLOR", required = true, allowableValues = {"COLOR", "SIZE"})
        @NotNull(message = "옵션 타입은 필수입니다")
        private OptionType type;

        @Schema(description = "옵션 값", example = "WHITE", required = true)
        @NotBlank(message = "옵션 값은 비어 있을 수 없습니다")
        @Size(max = 100, message = "옵션 값은 100자 이내여야 합니다")
        private String value;

        protected OptionDto() {}
    }
}
