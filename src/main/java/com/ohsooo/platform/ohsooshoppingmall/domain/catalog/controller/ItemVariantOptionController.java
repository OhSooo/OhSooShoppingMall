package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantOptionResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service.ItemVariantOptionService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalog/item-variant-option")
@RequiredArgsConstructor
@Tag(name = "ItemVariantOption", description = "상품 재고 옵션 조합 조회 API")
public class ItemVariantOptionController {

    private final ItemVariantOptionService itemVariantOptionService;

    // 특정 variant에 포함된 옵션 조회
    @Operation(
            summary = "재고 단위 옵션 조합 조회",
            description = "ItemVariant ID를 기준으로 해당 재고 단위에 포함된 옵션 조합을 조회합니다."
    )
    @GetMapping("/variant/{itemVariantId}")
    public ResponseEntity<BaseResponse<List<ItemVariantOptionResponse>>> getOptionsByVariant(
            @PathVariable Long itemVariantId
    ) {
        List<ItemVariantOptionResponse> response =
                itemVariantOptionService.findOptionsByVariantId(itemVariantId);

        return ResponseEntity.ok(
                BaseResponse.success("재고 옵션 조합 조회 성공", response)
        );
    }

    // 특정 variant + option 조합 존재 여부 조회
    @Operation(
            summary = "재고 옵션 조합 존재 여부 확인",
            description = "특정 재고(ItemVariant)에 특정 옵션이 포함되어 있는지 여부를 확인합니다."
    )
    @GetMapping("/variant/{itemVariantId}/option/{optionId}/exists")
    public ResponseEntity<BaseResponse<Boolean>> existsVariantOption(
            @PathVariable Long itemVariantId,
            @PathVariable Long optionId
    ) {
        boolean exists =
                itemVariantOptionService.existsVariantOption(itemVariantId, optionId);

        return ResponseEntity.ok(
                BaseResponse.success("재고 옵션 조합 존재 여부 조회 성공", exists)
        );
    }

    // 특정 option이 사용된 variant 조회 (관리/분석용)
    @Operation(
            summary = "옵션 기준 재고 조회",
            description = """
                    특정 옵션이 사용된 모든 재고(ItemVariant)를 조회합니다.
                    - 관리자/분석용 API
                    """
    )
    @GetMapping("/option/{optionId}")
    public ResponseEntity<BaseResponse<List<ItemVariantOptionResponse>>> getVariantsByOption(
            @PathVariable Long optionId
    ) {
        List<ItemVariantOptionResponse> response =
                itemVariantOptionService.findVariantsByOptionId(optionId);

        return ResponseEntity.ok(
                BaseResponse.success("옵션 기준 재고 조회 성공", response)
        );
    }
}