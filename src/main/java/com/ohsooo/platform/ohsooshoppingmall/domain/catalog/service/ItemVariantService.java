package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.ItemVariantMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ItemVariantService {

    private final ItemVariantRepository itemVariantRepository;
    private final ItemVariantMapper itemVariantMapper;
    private final ItemRepository itemRepository;

    // 아이템 존재 여부 확인
    private void validateItemExists(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new IllegalArgumentException("존재하지 않는 아이템입니다.");
        }
    }

    // ID로 단건 조회
    @Transactional(readOnly = true)
    public ItemVariantResponse findById(Long id) {
        ItemVariant itemVariant = itemVariantRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("상품 판매 단위를 찾을 수 없습니다"));
        return itemVariantMapper.toResponse(itemVariant);
    }

    // SKU로 단건 조회
    @Transactional(readOnly = true)
    public ItemVariantResponse findBySku(String sku) {
        ItemVariant itemVariant = itemVariantRepository.findBySku(sku)
                .orElseThrow(()-> new IllegalArgumentException("상품 판매 단위를 찾을 수 없습니다"));
        return itemVariantMapper.toResponse(itemVariant);
    }

    // 아이템 id 기준 조회
    @Transactional(readOnly = true)
    public List<ItemVariantResponse> findByItemId(Long itemId) {
        // 1. 아이템 존재 여부 확인
        validateItemExists(itemId);

        // 2. 상품 판매 단위 조회
        return itemVariantRepository.findByItem_ItemId(itemId)
                .stream()
                .map(itemVariantMapper::toResponse)
                .toList();
    }

    // 아이템 id + 상태 기준 조회
    @Transactional(readOnly = true)
    public List<ItemVariantResponse> findByItemIdAndStatus(Long itemId, ItemVariantStatus status) {
        // 1. 아이템 존재 여부 확인
        validateItemExists(itemId);

        // 2. 상품 판매 단위 조회
        return itemVariantRepository.findByItem_ItemIdAndStatus(itemId, status)
                .stream()
                .map(itemVariantMapper::toResponse)
                .toList();
    }

    // 판매 가능한 variant 조회(재고 있음)
    @Transactional(readOnly = true)
    public List<ItemVariantResponse> findBuyableVariantsByItemId(Long itemId, ItemVariantStatus status, int minQuantity) {      // 이거 함수 이름 추천 좀
        // 1. 아이템 존재 여부 확인
        validateItemExists(itemId);

        // 2. 상품 판매 단위 조회
        return itemVariantRepository.findByItem_ItemIdAndStatusAndQuantityGreaterThan(itemId, status, minQuantity)
                .stream()
                .map(itemVariantMapper::toResponse)
                .toList();
    }
}
