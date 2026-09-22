package com.finalproject.repository;

import com.finalproject.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    // Khuyến mãi toàn hệ thống (store_id IS NULL)
    List<Promotion> findByStoreIsNull();
    // Khuyến mãi riêng của 1 chi nhánh
    List<Promotion> findByStoreId(Long storeId);
}