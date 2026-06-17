package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;

@Getter
public class ItemCreateRequestDto {

    @Schema(description = "스토어 ID", example = "1", required = true)
    @NotNull(message = "storeId는 필수입니다")
    private Long storeId;

    @Schema(description = "카테고리 ID", example = "1", required = true)
    @NotNull(message = "categoryId는 필수입니다")
    private Long categoryId;

    @Schema(description = "상품명", example = "반팔 티셔츠", required = true)
    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 255, message = "상품명은 255자 이내여야 합니다")
    private String name;

    @Schema(description = "기본 가격", example = "20000", required = true)
    @NotNull(message = "basePrice는 필수입니다")
    @DecimalMin(value = "1.0", inclusive = true, message = "basePrice는 1 이상이어야 합니다")
    private BigDecimal basePrice;

    @Schema(description = "판매 단위(Variant) 목록", required = true)
    @NotEmpty(message = "variants는 하나 이상 필요합니다")
    @Valid
    private List<VariantDto> variants;

    protected ItemCreateRequestDto() {}

    @Getter
    public static class VariantDto {

        @Schema(description = "SKU (고유 식별자)", example = "TSHIRT-BLACK-M", required = true)
        @NotBlank(message = "sku는 필수입니다")
        @Size(max = 100, message = "sku는 100자 이내여야 합니다")
        private String sku;

        @Schema(description = "판매 가격", example = "20000", required = true)
        @NotNull(message = "price는 필수입니다")
        @DecimalMin(value = "1.0", inclusive = true, message = "basePrice는 1 이상이어야 합니다")
        private BigDecimal price;

        @Schema(description = "초기 재고 수량", example = "10", required = true)
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

        @Schema(description = "옵션 값", example = "BLACK", required = true)
        @NotBlank(message = "옵션 값은 비어 있을 수 없습니다")
        @Size(max = 100, message = "옵션 값은 100자 이내여야 합니다")
        private String value;

        protected OptionDto() {}
    }
}
