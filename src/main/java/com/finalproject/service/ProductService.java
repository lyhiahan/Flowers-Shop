package com.finalproject.service;

import com.finalproject.dto.request.ProductForm;
import com.finalproject.entity.Product;
import org.springframework.data.domain.Page;
import java.io.IOException;
import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product createProduct(ProductForm form) throws IOException;
    void deleteProduct(Long id);

    // Guest: Trên 10 sản phẩm bán chạy nhất sắp xếp giảm dần
    List<Product> getGuestFeaturedProducts();

    // Top 20 theo các tiêu chí
    List<Product> getTop20Newest();
    List<Product> getTop20BestSelling();
    List<Product> getTop20TopRated();
    List<Product> getTop20Wishlisted();

    // Phân trang tìm kiếm, danh mục, sắp xếp
    Page<Product> getProducts(Long categoryId, String keyword, String sort, int page, int size);

    // Sản phẩm của cửa hàng
    List<Product> getProductsByStore(Long storeId);

    // Wishlist (Sản phẩm yêu thích)
    boolean toggleWishlist(Long userId, Long productId);
    boolean isWishlisted(Long userId, Long productId);
    List<Product> getWishlist(Long userId);

    // Sản phẩm đã xem gần đây
    void recordRecentlyViewed(Long userId, Long productId);
    List<Product> getRecentlyViewed(Long userId);
}