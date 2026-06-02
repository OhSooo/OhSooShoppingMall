package com.ohsooo.platform.ohsooshoppingmall.domain.order.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.entity.OrderItemHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemHistoryRepository extends JpaRepository<OrderItemHistory, Long> {
}
