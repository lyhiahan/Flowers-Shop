package com.finalproject.repository;

import com.finalproject.entity.StoreInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {
    Optional<StoreInventory> findByStoreIdAndProductId(Long storeId, Long productId);

    // Tìm các chi nhánh khác còn hàng (> 0) cho 1 sản phẩm cụ thể
    @Query("SELECT si FROM StoreInventory si WHERE si.product.id = :productId AND si.store.id != :currentStoreId AND si.quantity > 0")
    List<StoreInventory> findAlternativeStoresWithStock(@Param("productId") Long productId, @Param("currentStoreId") Long currentStoreId);
}