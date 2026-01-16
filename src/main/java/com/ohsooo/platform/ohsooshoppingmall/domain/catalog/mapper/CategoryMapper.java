package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.CategoryResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category){
        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getDepth(),
                category.getDisplayOrder()
        );
    }
}
