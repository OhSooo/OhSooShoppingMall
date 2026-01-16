package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.CategoryResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service.CategoryService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog/category")
@RequiredArgsConstructor
@Tag(name = "Category", description="카테고리 조회 API")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "자식 카테고리 목록 조회",
            description = "특정 부모 카테고리의 자식 카테고리 목록을 조회합니다."
    )
    @GetMapping("/{categoryId}/children")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getChildrenCategories(
            @PathVariable Long categoryId
    ) {
        List<CategoryResponse> response = categoryService.getChildrenCategories(categoryId);
        return ResponseEntity.ok(BaseResponse.success("카테고리 조회 성공", response));
    }

    @Operation(
            summary = "depth 기준 카테고리 목록 조회",
            description = "depth 값에 해당하는 카테고리 목록을 조회합니다. depth가 없으면(null) 최상위 카테고리를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getCategoriesByDepth(
            @RequestParam(required = false) Integer depth
    ){
        List<CategoryResponse> response =
            depth == null
                    ? categoryService.getRootCategories()
                    : categoryService.getCategoriesByDepth(depth);
        return ResponseEntity.ok(BaseResponse.success("카테고리 조회 성공", response));
    }

    @Operation(
            summary = "카테고리 단건 조회",
            description = "특정 id의 카테고리를 조회합니다."
    )
    @GetMapping("/{categoryId}")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategory(
            @Parameter(description = "부모 카테고리 ID", example = "1")
            @PathVariable Long categoryId
    ) {
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(BaseResponse.success("카테고리 조회 성공", response));
    }
}
