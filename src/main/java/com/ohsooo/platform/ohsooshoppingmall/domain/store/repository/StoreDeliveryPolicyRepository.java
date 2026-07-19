package com.ohsooo.platform.ohsooshoppingmall.domain.store.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreDeliveryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreDeliveryPolicyRepository extends JpaRepository<StoreDeliveryPolicy, Long> {

    Optional<StoreDeliveryPolicy> findByStore_StoreId(Long storeId);
}
