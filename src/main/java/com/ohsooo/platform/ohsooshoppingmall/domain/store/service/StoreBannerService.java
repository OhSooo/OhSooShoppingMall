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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreBannerService {

    private final StoreBannerRepository storeBannerRepository;
    private final StoreService storeService;
    private final StoreMapper storeMapper;

    public StoreBannerResponse createBanner(Long storeId, Long ownerId,
                                            String imageUrl, String linkUrl, String title) {
        Store store = storeService.getStoreEntity(storeId);
        validateOwner(store, ownerId);

        int nextSortOrder = storeBannerRepository.findMaxSortOrderByStoreId(storeId) + 1;
        StoreBanner banner = new StoreBanner(store, imageUrl, linkUrl, title, nextSortOrder);
        StoreBanner saved = storeBannerRepository.save(banner);
        return storeMapper.toStoreBannerResponse(saved);
    }

    public StoreBannerResponse updateBanner(Long storeId, Long bannerId, Long ownerId,
                                            String imageUrl, String linkUrl, String title) {
        Store store = storeService.getStoreEntity(storeId);
        validateOwner(store, ownerId);

        StoreBanner banner = getBannerEntity(bannerId);
        validateBannerBelongsToStore(banner, storeId);

        banner.update(imageUrl, linkUrl, title);
        return storeMapper.toStoreBannerResponse(banner);
    }

    public List<StoreBannerResponse> reorderBanners(Long storeId, Long ownerId, List<Long> bannerIds) {
        Store store = storeService.getStoreEntity(storeId);
        validateOwner(store, ownerId);

        if (new HashSet<>(bannerIds).size() != bannerIds.size()) {
            throw new BusinessException(StoreErrorCode.STORE_BANNER_ORDER_DUPLICATED);
        }

        List<StoreBanner> banners = storeBannerRepository.findAllByStoreBannerIdIn(bannerIds);

        if (banners.size() != bannerIds.size()) {
            throw new BusinessException(StoreErrorCode.STORE_BANNER_NOT_FOUND);
        }

        Map<Long, StoreBanner> bannerMap = banners.stream()
                .collect(Collectors.toMap(StoreBanner::getStoreBannerId, Function.identity()));

        for (StoreBanner banner : banners) {
            if (!banner.getStore().getStoreId().equals(storeId)) {
                throw new BusinessException(StoreErrorCode.STORE_BANNER_NOT_BELONG_TO_STORE);
            }
        }

        Set<Long> storeBannerIds = storeBannerRepository
                .findAllByStore_StoreIdOrderBySortOrderAsc(storeId)
                .stream()
                .map(StoreBanner::getStoreBannerId)
                .collect(Collectors.toSet());
        if (!storeBannerIds.equals(new HashSet<>(bannerIds))) {
            throw new BusinessException(StoreErrorCode.STORE_BANNER_ORDER_MISMATCH);
        }

        for (int i = 0; i < bannerIds.size(); i++) {
            StoreBanner banner = bannerMap.get(bannerIds.get(i));
            banner.updateSortOrder(i);
        }

        return storeBannerRepository
                .findAllByStore_StoreIdOrderBySortOrderAsc(storeId)
                .stream()
                .map(storeMapper::toStoreBannerResponse)
                .toList();
    }

    public void deleteBanner(Long storeId, Long bannerId, Long ownerId) {
        Store store = storeService.getStoreEntity(storeId);
        validateOwner(store, ownerId);

        StoreBanner banner = getBannerEntity(bannerId);
        validateBannerBelongsToStore(banner, storeId);

        storeBannerRepository.delete(banner);
    }

    @Transactional(readOnly = true)
    public List<StoreBannerResponse> getBanners(Long storeId) {
        storeService.getStoreEntity(storeId);
        return storeBannerRepository
                .findAllByStore_StoreIdOrderBySortOrderAsc(storeId)
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
