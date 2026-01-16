package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.CategoryResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service.CategoryService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
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

    // 특정 부모의 자식 카테고리 조회
    @GetMapping("/{categoryId}/children")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getChildrenCategories(
            @PathVariable Long categoryId
    ) {
        List<CategoryResponse> response = categoryService.getChildrenCategories(categoryId);
        return ResponseEntity.ok(BaseResponse.success("카테고리 조회 성공", response));
    }

    // depth 기준 조회
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

    // category id 기준 조회
    @GetMapping("/{categoryId}")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategory(
            @PathVariable Long categoryId
    ) {
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(BaseResponse.success("카테고리 조회 성공", response));
    }
}
