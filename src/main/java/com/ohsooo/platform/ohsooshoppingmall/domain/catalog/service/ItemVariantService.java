package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.ItemErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.ItemVariantErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.ItemVariantMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
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

  private void validateItemExists(Long itemId) {
    if (!itemRepository.existsById(itemId)) {
      throw new BusinessException(ItemErrorCode.ITEM_NOT_FOUND);
    }
  }

  @Transactional(readOnly = true)
  public ItemVariantResponse findById(Long id) {
    ItemVariant itemVariant = itemVariantRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ItemVariantErrorCode.ITEM_VARIANT_NOT_FOUND));
    return itemVariantMapper.toResponse(itemVariant);
  }

  @Transactional(readOnly = true)
  public ItemVariantResponse findBySku(String sku) {
    ItemVariant itemVariant = itemVariantRepository.findBySku(sku)
        .orElseThrow(() -> new BusinessException(ItemVariantErrorCode.ITEM_VARIANT_NOT_FOUND));
    return itemVariantMapper.toResponse(itemVariant);
  }

  @Transactional(readOnly = true)
  public List<ItemVariantResponse> findByItemId(Long itemId) {
    validateItemExists(itemId);
    return itemVariantRepository.findByItem_ItemId(itemId)
        .stream()
        .map(itemVariantMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ItemVariantResponse> findByItemIdAndStatus(Long itemId, ItemVariantStatus status) {
    validateItemExists(itemId);
    return itemVariantRepository.findByItem_ItemIdAndStatus(itemId, status)
        .stream()
        .map(itemVariantMapper::toResponse)
        .toList();
  }

  // 판매 가능한 variant 조회: ACTIVE 상태 = 재고 있음 (InventoryService가 수량과 상태를 동기화함)
  @Transactional(readOnly = true)
  public List<ItemVariantResponse> findSalableVariantsByItemId(Long itemId) {
    validateItemExists(itemId);
    return itemVariantRepository.findByItem_ItemIdAndStatus(itemId, ItemVariantStatus.ACTIVE)
        .stream()
        .map(itemVariantMapper::toResponse)
        .toList();
  }
}
