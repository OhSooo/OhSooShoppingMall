package com.ohsooo.platform.ohsooshoppingmall.domain.store.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoreBannerRepository extends JpaRepository<StoreBanner, Long> {

    List<StoreBanner> findAllByStore_StoreIdOrderBySortOrderAsc(Long storeId);

    @Query("SELECT COALESCE(MAX(b.sortOrder), -1) FROM StoreBanner b WHERE b.store.storeId = :storeId")
    int findMaxSortOrderByStoreId(@Param("storeId") Long storeId);

    List<StoreBanner> findAllByStoreBannerIdIn(List<Long> bannerIds);
}
