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

  Optional<ItemVariant> findBySku(String sku);

  boolean existsBySku(String sku);

  List<ItemVariant> findByItem_ItemId(Long itemId);

  List<ItemVariant> findByItem_ItemIdAndStatus(Long itemId, ItemVariantStatus status);

  // Inventory 수량 변경 후 ItemVariant.status 동기화용. DISABLED 상태는 변경하지 않는다.
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE ItemVariant v
         SET v.status = :status
       WHERE v.itemVariantId = :itemVariantId
         AND v.status <> :disabledStatus
      """)
  void syncStatus(@Param("itemVariantId") Long itemVariantId,
                  @Param("status") ItemVariantStatus status,
                  @Param("disabledStatus") ItemVariantStatus disabledStatus);
}
