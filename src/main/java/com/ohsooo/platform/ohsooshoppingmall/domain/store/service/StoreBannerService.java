package com.ohsooo.platform.ohsooshoppingmall.domain.store.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreBannerResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreBanner;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.exception.StoreErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.mapper.StoreMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreBannerRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreBannerService {

    private final StoreBannerRepository storeBannerRepository;
    private final StoreService storeService;
    private final StoreMapper storeMapper;

    public StoreBannerResponse createBanner(Long storeId, Long ownerId,
                                            String imageUrl, String linkUrl, String title, Integer sortOrder) {
        Store store = storeService.getStoreEntity(storeId);
        validateOwner(store, ownerId);

        int order = sortOrder != null ? sortOrder : 0;
        StoreBanner banner = new StoreBanner(store, imageUrl, linkUrl, title, order);
        StoreBanner saved = storeBannerRepository.save(banner);
        return storeMapper.toStoreBannerResponse(saved);
    }

    public StoreBannerResponse updateBanner(Long storeId, Long bannerId, Long ownerId,
                                            String imageUrl, String linkUrl, String title,
                                            Integer sortOrder, Boolean isActive) {
        Store store = storeService.getStoreEntity(storeId);
        validateOwner(store, ownerId);

        StoreBanner banner = getBannerEntity(bannerId);
        validateBannerBelongsToStore(banner, storeId);

        banner.update(imageUrl, linkUrl, title, sortOrder, isActive);
        return storeMapper.toStoreBannerResponse(banner);
    }

    public void deactivateBanner(Long storeId, Long bannerId, Long ownerId) {
        Store store = storeService.getStoreEntity(storeId);
        validateOwner(store, ownerId);

        StoreBanner banner = getBannerEntity(bannerId);
        validateBannerBelongsToStore(banner, storeId);

        banner.deactivate();
    }

    @Transactional(readOnly = true)
    public List<StoreBannerResponse> getActiveBanners(Long storeId) {
        storeService.getStoreEntity(storeId);
        return storeBannerRepository
                .findAllByStore_StoreIdAndIsActiveTrueOrderBySortOrderAsc(storeId)
                .stream()
                .map(storeMapper::toStoreBannerResponse)
                .toList();
    }

    private StoreBanner getBannerEntity(Long bannerId) {
        return storeBannerRepository.findById(bannerId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_BANNER_NOT_FOUND));
    }

    private void validateBannerBelongsToStore(StoreBanner banner, Long storeId) {
        if (!banner.getStore().getStoreId().equals(storeId)) {
            throw new BusinessException(StoreErrorCode.STORE_BANNER_NOT_BELONG_TO_STORE);
        }
    }

    private void validateOwner(Store store, Long ownerId) {
        if (!store.getOwnerId().equals(ownerId)) {
            throw new BusinessException(StoreErrorCode.STORE_OWNER_FORBIDDEN);
        }
    }
}
