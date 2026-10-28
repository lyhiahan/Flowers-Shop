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
    Optional<Voucher> findByCodeAndIsActiveTrue(String code);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.code = :code AND v.isActive = true")
    Optional<Voucher> findByCodeAndIsActiveTrueForUpdate(@org.springframework.data.repository.query.Param("code") String code);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id")
    Optional<Voucher> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @Query("SELECT v FROM Voucher v WHERE v.isActive = true AND v.startDate <= :today AND v.endDate >= :today AND (v.store.id = :storeId OR v.store IS NULL) AND v.usedCount < v.quantity")
    List<Voucher> findAvailableVouchers(Long storeId, LocalDate today);

    List<Voucher> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<Voucher> findByStoreIsNullOrderByCreatedAtDesc();
}
