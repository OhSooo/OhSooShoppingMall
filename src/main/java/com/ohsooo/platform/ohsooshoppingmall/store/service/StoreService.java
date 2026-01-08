package com.ohsooo.platform.ohsooshoppingmall.store.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ohsooo.platform.ohsooshoppingmall.store.domain.Store;
import com.ohsooo.platform.ohsooshoppingmall.store.repository.StoreRepository;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store createStore(Long ownerId, String name, String description) {
        Store store = new Store(ownerId, name, description);
        return storeRepository.save(store);
    }

    @Transactional(readOnly = true)
    public Store getStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("스토어가 존재하지 않습니다"));
    }
}