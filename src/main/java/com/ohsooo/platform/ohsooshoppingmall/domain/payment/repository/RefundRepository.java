package com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Refund;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long> {

  List<Refund> findAllByPayment_PaymentIdOrderByCreatedAtDesc(Long paymentId);

  @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.payment.paymentId = :paymentId AND r.status = com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.RefundStatus.SUCCEEDED")
  BigDecimal sumSucceededAmountByPaymentId(@Param("paymentId") Long paymentId);
}
