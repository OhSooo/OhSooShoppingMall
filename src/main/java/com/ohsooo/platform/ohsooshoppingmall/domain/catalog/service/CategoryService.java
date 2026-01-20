package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.CategoryResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Category;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.CategoryMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // 최상위 카테고리 조회
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository
                .findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    // 특정 부모의 자식 카테고리 조회
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildrenCategories(Long parentCategoryId) {
        return categoryRepository
                .findByParent_CategoryIdAndIsActiveTrueOrderByDisplayOrderAsc(parentCategoryId)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    // depth 기준 조회
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByDepth(Integer depth) {
        return categoryRepository
                .findByDepthAndIsActiveTrueOrderByDisplayOrderAsc(depth)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    // id 기준 카테고리 조회
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.
                findByCategoryIdAndIsActiveTrue(id)
                .orElseThrow(()-> new IllegalArgumentException("카테고리를 찾을 수 없습니다"));

        return categoryMapper.toResponse(category);
    }
}
