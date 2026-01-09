package com.ohsooo.platform.ohsooshoppingmall.domain.store.mapper;


import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import org.springframework.stereotype.Component;

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


}
