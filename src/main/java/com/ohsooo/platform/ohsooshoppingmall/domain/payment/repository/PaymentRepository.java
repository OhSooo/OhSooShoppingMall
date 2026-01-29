package com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByOrderId(Long orderId);

  Optional<Payment> findByPgPaymentKey(String pgPaymentKey);

  Optional<Payment> findByPgTransactionId(String pgTransactionId);

  boolean existsByPgPaymentKey(String pgPaymentKey);

  boolean existsByPgTransactionId(String pgTransactionId);
}
