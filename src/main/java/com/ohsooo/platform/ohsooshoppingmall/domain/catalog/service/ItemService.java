package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Category;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.CategoryErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.ItemErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.ItemMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.CategoryRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.exception.StoreErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    // 단건 조회
    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        Item item = itemRepository.findByItemIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
        return itemMapper.toResponse(item);
    }

    // 카테고리 기준 조회
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByCategory(Long categoryId) {

        // 1. category 존재 여부 확인
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }

        // 2. 상품 조회
        return itemRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId)
                .stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    // 스토어 기준 조회
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByStore(Long storeId) {

        // 1. store 존재 여부 확인
        if (!storeRepository.existsById(storeId)) {
            throw new BusinessException(StoreErrorCode.STORE_NOT_FOUND);
        }

        // 2. 상품 조회
        return itemRepository.findByStore_StoreIdAndIsDeletedFalse(storeId)
                .stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    // 기본 조회(정렬 / 페이징용)
    @Transactional(readOnly = true)
    public Page<ItemResponse> getItems(Pageable pageable) {
        return itemRepository.findByIsDeletedFalse(pageable)
                .map(itemMapper::toResponse);
    }
}
