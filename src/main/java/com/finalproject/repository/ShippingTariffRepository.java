package com.finalproject.repository;

import com.finalproject.entity.ShippingTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingTariffRepository extends JpaRepository<ShippingTariff, Long> {
    List<ShippingTariff> findByIsDeletedFalseOrderByIdAsc();
    List<ShippingTariff> findByIsDeletedFalseAndIsActiveTrue();
    List<ShippingTariff> findByStoreIdAndIsDeletedFalse(Long storeId);

    @Query("SELECT t FROM ShippingTariff t WHERE t.isDeleted = false " +
           "AND (:storeId IS NULL OR t.store.id = :storeId OR t.store IS NULL) " +
           "AND (:service IS NULL OR :service = 'ALL' OR t.serviceName = :service) " +
           "ORDER BY t.id ASC")
    List<ShippingTariff> searchTariffs(@Param("storeId") Long storeId, @Param("service") String service);
}
