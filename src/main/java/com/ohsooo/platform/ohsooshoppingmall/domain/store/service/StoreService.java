package com.ohsooo.platform.ohsooshoppingmall.domain.store.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreDetailResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreProfileResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreBanner;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreDeliveryPolicy;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreOperationStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.exception.StoreErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.mapper.StoreMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreBannerRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreDeliveryPolicyRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreRepository;
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
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreBannerRepository storeBannerRepository;
    private final StoreDeliveryPolicyRepository storeDeliveryPolicyRepository;
    private final StoreMapper storeMapper;

    public StoreResponse createStore(Long ownerId, String name, String description) {
        Store store = new Store(ownerId, name, description);
        Store saved = storeRepository.save(store);
        return storeMapper.toStoreResponse(saved);
    }

    @Transactional(readOnly = true)
    public StoreDetailResponse getStoreDetail(Long storeId) {
        Store store = getStoreEntity(storeId);
        List<StoreBanner> banners = storeBannerRepository
                .findAllByStore_StoreIdAndIsActiveTrueOrderBySortOrderAsc(storeId);
        StoreDeliveryPolicy deliveryPolicy = storeDeliveryPolicyRepository
                .findByStore_StoreId(storeId)
                .orElse(null);
        return storeMapper.toStoreDetailResponse(store, banners, deliveryPolicy);
    }

    @Transactional(readOnly = true)
    public Store getStoreEntity(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getActiveStores() {
        List<Store> storeList = storeRepository.findAllByStatus(StoreStatus.ACTIVE);
        return storeList.stream().map(storeMapper::toStoreResponse).toList();
    }

    public StoreProfileResponse updateProfile(Long storeId, Long ownerId,
                                               String description, String notice, String mainImageUrl) {
        Store store = getStoreEntity(storeId);
        validateOwner(store, ownerId);
        store.updateProfile(description, notice, mainImageUrl);
        return storeMapper.toStoreProfileResponse(store);
    }

    public StoreProfileResponse changeOperationStatus(Long storeId, Long ownerId,
                                                       StoreOperationStatus operationStatus) {
        Store store = getStoreEntity(storeId);
        validateOwner(store, ownerId);
        store.changeOperationStatus(operationStatus);
        return storeMapper.toStoreProfileResponse(store);
    }

    public void changeStatusByAdmin(Long storeId, StoreStatus newStatus) {
        Store store = getStoreEntity(storeId);
        store.changeStatus(newStatus);
    }

    private void validateOwner(Store store, Long ownerId) {
        if (!store.getOwnerId().equals(ownerId)) {
            throw new BusinessException(StoreErrorCode.STORE_OWNER_FORBIDDEN);
        }
    }
}