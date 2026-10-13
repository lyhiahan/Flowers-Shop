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
    interface CatalogStockRow {
        Long getProductId();
        Long getStoreId();
        Integer getQuantity();
    }

    // One projection query for all visible cards, respecting the same branch rules as product detail.
    @Query("""
            SELECT p.id AS productId, s.id AS storeId, si.quantity AS quantity
            FROM StoreInventory si JOIN si.product p JOIN si.store s LEFT JOIN p.store ownerStore
            WHERE p.id IN :productIds AND s.status = true AND s.isOpen = true
                AND p.isActive = true AND p.isSelling = true
                AND (p.isDeleted = false OR p.isDeleted IS NULL)
                AND (ownerStore.id IS NULL OR ownerStore.id = s.id)
                AND (:storeId IS NULL OR s.id = :storeId) AND si.quantity > 0
            ORDER BY p.id, s.id
            """)
    List<CatalogStockRow> findCatalogStock(@Param("productIds") List<Long> productIds,
                                         @Param("storeId") Long storeId);

    // Public KPI: only stock that a customer can actually buy, not private/closed inventory.
    @Query("""
            SELECT COALESCE(SUM(CAST(si.quantity AS long)), 0)
            FROM StoreInventory si JOIN si.product p JOIN si.store s LEFT JOIN p.store ownerStore
            WHERE s.status = true AND s.isOpen = true AND p.isActive = true AND p.isSelling = true
                AND (p.isDeleted = false OR p.isDeleted IS NULL)
                AND (ownerStore.id IS NULL OR ownerStore.id = s.id)
                AND (:storeId IS NULL OR s.id = :storeId) AND si.quantity > 0
            """)
    long sumCatalogQuantity(@Param("storeId") Long storeId);

    List<StoreInventory> findByStoreIdAndProductIdIn(Long storeId, List<Long> productIds);

    Optional<StoreInventory> findByStoreIdAndProductId(Long storeId, Long productId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM StoreInventory si WHERE si.store.id = :storeId AND si.product.id = :productId")
    Optional<StoreInventory> findLockedStock(@Param("storeId") Long storeId, @Param("productId") Long productId);

    List<StoreInventory> findByStoreId(Long storeId);

    List<StoreInventory> findByProductId(Long productId);

    // Quantity already excludes units reserved by successful orders, including unpaid COD.
    // A stock snapshot must not be filtered by product creation date or report period.
    @Query("""
            SELECT COALESCE(SUM(CAST(si.quantity AS long)), 0)
            FROM StoreInventory si JOIN si.product p
            WHERE (:storeId IS NULL OR si.store.id = :storeId)
                AND (p.isDeleted = false OR p.isDeleted IS NULL)
            """)
    long sumRemainingQuantity(@Param("storeId") Long storeId);

    long countByStoreIdAndQuantityLessThanEqual(Long storeId, Integer threshold);

    // Tìm các chi nhánh khác còn hàng (> 0) cho 1 sản phẩm cụ thể
    @Query("""
            SELECT si FROM StoreInventory si JOIN si.product p JOIN si.store s LEFT JOIN p.store ownerStore
            WHERE p.id = :productId AND s.id != :currentStoreId AND si.quantity > 0
                AND s.status = true AND s.isOpen = true AND p.isActive = true AND p.isSelling = true
                AND (p.isDeleted = false OR p.isDeleted IS NULL)
                AND (ownerStore.id IS NULL OR ownerStore.id = s.id)
            """)
    List<StoreInventory> findAlternativeStoresWithStock(@Param("productId") Long productId, @Param("currentStoreId") Long currentStoreId);
}
