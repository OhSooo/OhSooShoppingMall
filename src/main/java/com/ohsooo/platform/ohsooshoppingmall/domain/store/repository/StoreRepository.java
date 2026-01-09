package com.ohsooo.platform.ohsooshoppingmall.domain.store.repository;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findAllByStatus(StoreStatus status);
}
