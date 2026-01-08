package com.ohsooo.platform.ohsooshoppingmall.store.repository;
import com.ohsooo.platform.ohsooshoppingmall.store.domain.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ohsooo.platform.ohsooshoppingmall.store.domain.Store;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findAllByStatus(StoreStatus status);
}
