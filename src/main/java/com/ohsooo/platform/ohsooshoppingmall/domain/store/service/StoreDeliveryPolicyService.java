package com.ohsooo.platform.ohsooshoppingmall.domain.store.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreDeliveryPolicyResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreDeliveryPolicy;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.exception.StoreErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.mapper.StoreMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreDeliveryPolicyRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreDeliveryPolicyService {

    private final StoreDeliveryPolicyRepository storeDeliveryPolicyRepository;
    private final StoreService storeService;
    private final StoreMapper storeMapper;

    @Transactional(readOnly = true)
    public StoreDeliveryPolicyResponse getDeliveryPolicy(Long storeId) {
        storeService.getStoreEntity(storeId);
        StoreDeliveryPolicy policy = storeDeliveryPolicyRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_DELIVERY_POLICY_NOT_FOUND));
        return storeMapper.toStoreDeliveryPolicyResponse(policy);
    }

    @Transactional(readOnly = true)
    public StoreDeliveryPolicyResponse getDeliveryPolicyOrNull(Long storeId) {
        return storeDeliveryPolicyRepository.findByStore_StoreId(storeId)
                .map(storeMapper::toStoreDeliveryPolicyResponse)
                .orElse(null);
    }

    public StoreDeliveryPolicyResponse upsertDeliveryPolicy(Long storeId, Long ownerId,
                                                             BigDecimal baseDeliveryFee,
                                                             BigDecimal freeDeliveryThreshold) {
        Store store = storeService.getStoreEntity(storeId);
        validateOwner(store, ownerId);

        StoreDeliveryPolicy policy = storeDeliveryPolicyRepository.findByStore_StoreId(storeId)
                .map(existing -> {
                    existing.update(baseDeliveryFee, freeDeliveryThreshold);
                    return existing;
                })
                .orElseGet(() -> {
                    StoreDeliveryPolicy newPolicy = new StoreDeliveryPolicy(store, baseDeliveryFee, freeDeliveryThreshold);
                    return storeDeliveryPolicyRepository.save(newPolicy);
                });

        return storeMapper.toStoreDeliveryPolicyResponse(policy);
    }

    private void validateOwner(Store store, Long ownerId) {
        if (!store.getOwnerId().equals(ownerId)) {
            throw new BusinessException(StoreErrorCode.STORE_OWNER_FORBIDDEN);
        }
    }
}
