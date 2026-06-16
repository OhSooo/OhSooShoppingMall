package com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByOrderId(Long orderId);

  Optional<Payment> findByPgPaymentKey(String pgPaymentKey);

  Optional<Payment> findByPgTransactionId(String pgTransactionId);

  boolean existsByPgPaymentKey(String pgPaymentKey);

  boolean existsByPgTransactionId(String pgTransactionId);

  /** 동일 주문에 READY/CAPTURED 결제가 이미 있는지 확인 (P-2 중복 방지) */
  boolean existsByOrderIdAndStatusIn(Long orderId, List<PaymentStatus> statuses);

  /** 동시 confirm 요청 방어를 위한 비관적 쓰기 락 조회 (P-3) */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId")
  Optional<Payment> findByIdWithLock(@Param("paymentId") Long paymentId);
}
