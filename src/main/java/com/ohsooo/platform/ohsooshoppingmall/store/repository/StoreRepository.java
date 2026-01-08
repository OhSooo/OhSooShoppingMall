package com.ohsooo.platform.ohsooshoppingmall.store.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ohsooo.platform.ohsooshoppingmall.store.domain.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
