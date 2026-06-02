package com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Refund;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {

  List<Refund> findAllByPayment_PaymentIdOrderByCreatedAtDesc(Long paymentId);
}
