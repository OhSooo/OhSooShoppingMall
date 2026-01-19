package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Category;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // 단건 조회
    Optional<Item> findByItemIdAndIsDeletedFalse(Long itemId);

    // 카테고리 기준 조회
    List<Item> findByCategory_CategoryIdAndIsDeletedFalse(Long categoryId);

    // 스토어 기준 조회
    List<Item> findByStore_StoreIdAndIsDeletedFalse(Long storeId);

    // 기본 조회 (정렬 / 페이징용)
    Page<Item> findByIsDeletedFalse(Pageable pageable);
}
