package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemVariantRepository extends JpaRepository<ItemVariant, Long> {

    // 단건 조회(ID)
    Optional<ItemVariant> findByItemVariantId(Long itemVariantId);

    // 단건 조회(SKU)
    Optional<ItemVariant> findBySku(String sku);

    // 아이템 id 기준 조회
    List<ItemVariant> findByItem_ItemId(Long itemId);

    // 아이템 id + 상태 기준 조회
    List<ItemVariant> findByItem_ItemIdAndStatus(Long itemId, ItemVariantStatus status);

    // 판매 가능한 variant 조회(재고 있음)
    List<ItemVariant> findByItem_ItemIdAndStatusAndQuantityGreaterThan(Long itemId, ItemVariantStatus status, int quantity);
}
