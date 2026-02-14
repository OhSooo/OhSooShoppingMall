package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.ItemMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.CategoryRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        Item item = itemRepository.findByItemIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));
        return itemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다.");
        }

        return itemRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId)
            .stream()
            .map(itemMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByStore(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다.");
        }

        return itemRepository.findByStore_StoreIdAndIsDeletedFalse(storeId)
            .stream()
            .map(itemMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> getItems(Pageable pageable) {
        return itemRepository.findByIsDeletedFalse(pageable)
            .map(itemMapper::toResponse);
    }
}
