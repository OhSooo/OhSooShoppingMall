package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.CreateOptionRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.OptionResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.OptionType;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service.OptionService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/catalog/option")
@RequiredArgsConstructor
@Tag(name="Option", description="옵션 관련 API")
public class OptionController {

    private final OptionService optionService;

    @Operation(
            summary = "옵션 단건 조회",
            description = "옵션 ID를 기준으로 옵션 정보를 조회합니다."
    )
    @GetMapping("/{optionId}")
    public ResponseEntity<BaseResponse<OptionResponse>> getOption(
            @PathVariable Long optionId
    ) {
        OptionResponse response = optionService.findOptionById(optionId);
        return ResponseEntity.ok(BaseResponse.success("옵션 조회 성공", response));
    }

    @Operation(
            summary = "상품 기준 옵션 조회",
            description = "상품 ID를 기준으로 해당 상품에 등록된 모든 옵션을 조회합니다."
    )
    @GetMapping("/item/{itemId}")
    public ResponseEntity<BaseResponse<List<OptionResponse>>> getOptionsByItem(
            @PathVariable Long itemId
    ) {
        List<OptionResponse> response = optionService.findOptionsByItemId(itemId);
        return ResponseEntity.ok(BaseResponse.success("옵션 조회 성공", response));
    }

    @Operation(
            summary = "상품 + 옵션 타입 기준 옵션 조회",
            description = "상품 ID와 옵션 타입(COLOR, SIZE 등)을 기준으로 옵션 목록을 조회합니다."
    )
    @GetMapping("/item/{itemId}/type/{type}")
    public ResponseEntity<BaseResponse<List<OptionResponse>>> getOptionsByItemAndType(
            @PathVariable Long itemId,
            @PathVariable OptionType type
    ) {
        List<OptionResponse> response = optionService.findOptionsByItemIdAndType(itemId, type);
        return ResponseEntity.ok(BaseResponse.success("옵션 조회 성공", response));
    }

    @Operation(
            summary = "옵션 생성",
            description = """
                    특정 상품에 새로운 옵션을 생성합니다.
                    - 동일한 상품에 동일한 옵션 타입 + 값은 중복 생성할 수 없습니다.
                    - 판매자 권한이 필요합니다.
                    """
    )
    @PostMapping
    public  ResponseEntity<BaseResponse<Void>> createOption(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CreateOptionRequest request
    ) {
        optionService.createOption(
                userId,
                request.getItemId(),
                request.getType(),
                request.getValue()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success("옵션 생성 성공", null));
    }
}
