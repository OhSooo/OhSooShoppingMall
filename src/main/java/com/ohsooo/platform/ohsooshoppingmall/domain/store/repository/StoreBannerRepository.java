package com.ohsooo.platform.ohsooshoppingmall.domain.store.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreBannerRepository extends JpaRepository<StoreBanner, Long> {

    List<StoreBanner> findAllByStore_StoreIdAndIsActiveTrueOrderBySortOrderAsc(Long storeId);

    List<StoreBanner> findAllByStore_StoreIdOrderBySortOrderAsc(Long storeId);
}
