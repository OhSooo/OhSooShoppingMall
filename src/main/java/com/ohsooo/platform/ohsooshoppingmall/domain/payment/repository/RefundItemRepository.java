package com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.RefundItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

  List<RefundItem> findAllByRefund_RefundId(Long refundId);
}
