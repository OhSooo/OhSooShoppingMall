package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  Optional<Inventory> findByItemVariantId(Long itemVariantId);

  // adjustStock의 read-then-write 경쟁 조건 방지용 비관적 락 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Inventory i WHERE i.itemVariantId = :itemVariantId")
  Optional<Inventory> findByItemVariantIdWithLock(@Param("itemVariantId") Long itemVariantId);

  // 동시성 안전 재고 차감: quantity >= amount 일 때만 감소, 성공 시 1 반환
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE Inventory i
         SET i.quantity = i.quantity - :amount
       WHERE i.itemVariantId = :itemVariantId
         AND i.quantity >= :amount
      """)
  int decreaseStockIfEnough(@Param("itemVariantId") Long itemVariantId, @Param("amount") int amount);

  // 재고 증가 (취소 복원 / 입고)
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE Inventory i
         SET i.quantity = i.quantity + :amount
       WHERE i.itemVariantId = :itemVariantId
      """)
  int increaseStock(@Param("itemVariantId") Long itemVariantId, @Param("amount") int amount);
}
