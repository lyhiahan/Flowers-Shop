package com.finalproject.repository;

import com.finalproject.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Voucher> findByCodeAndIsActiveTrue(String code);

    @Query("SELECT v FROM Voucher v WHERE v.isActive = true AND v.startDate <= :today AND v.endDate >= :today AND (v.store.id = :storeId OR v.store IS NULL) AND v.usedCount < v.quantity")
    List<Voucher> findAvailableVouchers(Long storeId, LocalDate today);

    List<Voucher> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<Voucher> findByStoreIsNullOrderByCreatedAtDesc();
}
