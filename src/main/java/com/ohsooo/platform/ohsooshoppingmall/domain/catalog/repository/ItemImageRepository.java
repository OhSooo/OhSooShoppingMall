package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    List<ItemImage> findByItem_ItemIdOrderByDisplayOrderAscItemImageIdAsc(Long itemId);

    Optional<ItemImage> findByItemImageIdAndItem_ItemId(Long imageId, Long itemId);

    List<ItemImage> findByItem_ItemIdAndIsPrimaryTrue(Long itemId);

    boolean existsByItem_ItemIdAndIsPrimaryTrue(Long itemId);

    Optional<ItemImage> findFirstByItem_ItemIdOrderByDisplayOrderAscItemImageIdAsc(Long itemId);

    int countByItem_ItemId(Long itemId);

    void deleteAllByItem_ItemId(Long itemId);

    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) FROM ItemImage i WHERE i.item.itemId = :itemId")
    int findMaxDisplayOrderByItemId(@Param("itemId") Long itemId);

    List<ItemImage> findByItem_ItemIdAndItemImageIdIn(Long itemId, List<Long> imageIds);

    @Modifying
    @Query("UPDATE ItemImage i SET i.isPrimary = false, i.updatedAt = CURRENT_TIMESTAMP "
         + "WHERE i.item.itemId = :itemId AND i.isPrimary = true")
    int clearPrimaryByItemId(@Param("itemId") Long itemId);

    @Modifying
    @Query("UPDATE ItemImage i SET i.isPrimary = false, i.updatedAt = CURRENT_TIMESTAMP "
         + "WHERE i.item.itemId = :itemId AND i.isPrimary = true AND i.itemImageId <> :excludeImageId")
    int clearPrimaryByItemIdExcluding(@Param("itemId") Long itemId, @Param("excludeImageId") Long excludeImageId);
}
