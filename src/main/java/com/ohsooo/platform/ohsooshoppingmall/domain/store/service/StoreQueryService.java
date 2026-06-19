package com.ohsooo.platform.ohsooshoppingmall.domain.store.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreBannerResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreDeliveryPolicyResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreDetailResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreProfileResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreQueryService {

    private final StoreService storeService;
    private final StoreBannerService storeBannerService;
    private final StoreDeliveryPolicyService storeDeliveryPolicyService;
    private final StoreMapper storeMapper;

    public StoreDetailResponse getStoreDetail(Long storeId) {
        StoreProfileResponse profile = storeService.getStoreProfile(storeId);
        List<StoreBannerResponse> banners = storeBannerService.getActiveBanners(storeId);
        StoreDeliveryPolicyResponse deliveryPolicy =
                storeDeliveryPolicyService.getDeliveryPolicyOrNull(storeId);

        return storeMapper.toStoreDetailResponse(profile, banners, deliveryPolicy);
    }
}
