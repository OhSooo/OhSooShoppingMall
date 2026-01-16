package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 최상위 카테고리 조회(parent 없음)
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    // 특정 부모의 자식 카테고리 조회
    List<Category> findByParent_CategoryIdAndIsActiveTrueOrderByDisplayOrderAsc(Long parentId);

    // depth 기준 조회
    List<Category> findByDepthAndIsActiveTrueOrderByDisplayOrderAsc(int depth);

    // category id 기준 조회
    Optional<Category> findByCategoryIdAndIsActiveTrue(Long categoryId);
}
