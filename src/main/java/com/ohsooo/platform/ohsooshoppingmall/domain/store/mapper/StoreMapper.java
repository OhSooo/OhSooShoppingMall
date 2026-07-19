package com.ohsooo.platform.ohsooshoppingmall.domain.store.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.*;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreBanner;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreDeliveryPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class StoreMapper {

    public StoreResponse toStoreResponse(Store store) {
        return StoreResponse.builder()
                .storeId(store.getStoreId())
                .ownerId(store.getOwnerId())
                .name(store.getName())
                .description(store.getDescription())
                .status(store.getStatus())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    public StoreProfileResponse toStoreProfileResponse(Store store) {
        return StoreProfileResponse.builder()
                .storeId(store.getStoreId())
                .ownerId(store.getOwnerId())
                .name(store.getName())
                .description(store.getDescription())
                .notice(store.getNotice())
                .mainImageUrl(store.getMainImageUrl())
                .status(store.getStatus())
                .operationStatus(store.getOperationStatus())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    public StoreBannerResponse toStoreBannerResponse(StoreBanner banner) {
        return StoreBannerResponse.builder()
                .storeBannerId(banner.getStoreBannerId())
                .storeId(banner.getStore().getStoreId())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .title(banner.getTitle())
                .sortOrder(banner.getSortOrder())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }

    public StoreDeliveryPolicyResponse toStoreDeliveryPolicyResponse(StoreDeliveryPolicy policy) {
        return StoreDeliveryPolicyResponse.builder()
                .deliveryPolicyId(policy.getDeliveryPolicyId())
                .storeId(policy.getStore().getStoreId())
                .baseDeliveryFee(policy.getBaseDeliveryFee())
                .freeDeliveryThreshold(policy.getFreeDeliveryThreshold())
                .isActive(policy.isActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    public StoreDetailResponse toStoreDetailResponse(
            StoreProfileResponse profile,
            List<StoreBannerResponse> banners,
            StoreDeliveryPolicyResponse deliveryPolicy
    ) {
        return StoreDetailResponse.builder()
                .storeId(profile.getStoreId())
                .ownerId(profile.getOwnerId())
                .name(profile.getName())
                .description(profile.getDescription())
                .notice(profile.getNotice())
                .mainImageUrl(profile.getMainImageUrl())
                .status(profile.getStatus())
                .operationStatus(profile.getOperationStatus())
                .banners(Objects.requireNonNullElseGet(banners, List::of))
                .deliveryPolicy(deliveryPolicy)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
