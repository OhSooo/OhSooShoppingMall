package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemVariantRepository extends JpaRepository<ItemVariant, Long> {

  // 단건 조회(SKU)
  Optional<ItemVariant> findBySku(String sku);

  // 아이템 id 기준 조회
  List<ItemVariant> findByItem_ItemId(Long itemId);

  // 아이템 id + 상태 기준 조회
  List<ItemVariant> findByItem_ItemIdAndStatus(Long itemId, ItemVariantStatus status);

  // 판매 가능한 variant 조회(재고 있음)
  List<ItemVariant> findByItem_ItemIdAndStatusAndQuantityGreaterThan(
      Long itemId, ItemVariantStatus status, int quantity);

  /**
   * 동시성 안전 재고 차감 (원자 UPDATE)
   * - quantity가 충분할 때만 감소
   * - 성공하면 1, 실패하면 0 반환
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
        UPDATE ItemVariant v
           SET v.quantity = v.quantity - :amount
         WHERE v.itemVariantId = :variantId
           AND v.quantity >= :amount
    """)
  int decreaseStockIfEnough(@Param("variantId") Long variantId, @Param("amount") int amount);

  /**
   * 재고 증가 (입고/취소복원 등)
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
        UPDATE ItemVariant v
           SET v.quantity = v.quantity + :amount
         WHERE v.itemVariantId = :variantId
    """)
  int increaseStock(@Param("variantId") Long variantId, @Param("amount") int amount);
}
