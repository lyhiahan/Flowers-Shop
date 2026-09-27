package com.finalproject.repository;

import com.finalproject.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByName(@Param("keyword") String keyword);

    // Phân trang theo danh mục
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // Phân trang tìm kiếm theo từ khóa (tên hoặc mô tả)
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String desc, Pageable pageable);

    // Phân trang kết hợp cả danh mục và từ khóa
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByCategoryAndKeyword(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    // Guest: Sản phẩm có số lượng bán trên 10, sắp xếp giảm dần
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isSelling = true AND p.sold >= 10 ORDER BY p.sold DESC")
    List<Product> findGuestFeaturedProducts();

    // Top 20 Mới nhất
    List<Product> findTop20ByIsActiveTrueAndIsSellingTrueOrderByCreatedAtDesc();

    // Top 20 Bán chạy nhất
    List<Product> findTop20ByIsActiveTrueAndIsSellingTrueOrderBySoldDesc();

    // Top 20 Đánh giá cao nhất
    List<Product> findTop20ByIsActiveTrueAndIsSellingTrueOrderByRatingDesc();

    // Sản phẩm của cửa hàng
    List<Product> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Product> findByStoreIdAndNameContainingIgnoreCase(Long storeId, String name);
    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    // Tìm kiếm toàn sàn phân trang
    Page<Product> findByIsActiveTrueAndIsSellingTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isSelling = true " +
           "AND (:categoryIds IS NULL OR p.category.id IN :categoryIds) " +
           "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:minPrice IS NULL OR p.priceBuy >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.priceBuy <= :maxPrice) " +
           "AND (:minRating IS NULL OR p.rating >= :minRating) " +
           "AND (:storeId IS NULL OR p.store.id = :storeId)")
    Page<Product> filterProducts(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("keyword") String keyword,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("storeId") Long storeId,
            Pageable pageable);
}