package com.finalproject.service.impl;

import com.finalproject.dto.request.ProductForm;
import com.finalproject.entity.*;
import com.finalproject.exception.ResourceNotFoundException;
import com.finalproject.repository.*;
import com.finalproject.service.CloudinaryService;
import com.finalproject.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final StoreRepository storeRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductWishlistRepository wishlistRepository;
    private final RecentlyViewedRepository recentlyViewedRepository;
    private final UserRepository userRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có ID: " + id));
    }

    @Override
    public List<Product> getGuestFeaturedProducts() {
        List<Product> featured = productRepository.findGuestFeaturedProducts();
        if (featured.isEmpty()) {
            // Fallback nếu dữ liệu mẫu chưa có sold >= 10: lấy tất cả sắp xếp theo giá hoặc ID
            return productRepository.findAll();
        }
        return featured;
    }

    @Override
    public List<Product> getTop20Newest() {
        return productRepository.findTop20ByIsActiveTrueAndIsSellingTrueOrderByCreatedAtDesc();
    }

    @Override
    public List<Product> getTop20BestSelling() {
        return productRepository.findTop20ByIsActiveTrueAndIsSellingTrueOrderBySoldDesc();
    }

    @Override
    public List<Product> getTop20TopRated() {
        return productRepository.findTop20ByIsActiveTrueAndIsSellingTrueOrderByRatingDesc();
    }

    @Override
    public List<Product> getTop20Wishlisted() {
        // Lấy danh sách sản phẩm bán chạy hoặc rating cao làm top yêu thích
        return productRepository.findTop20ByIsActiveTrueAndIsSellingTrueOrderByRatingDesc();
    }

    @Override
    public org.springframework.data.domain.Page<Product> getProducts(Long categoryId, String keyword, String sort, int page, int size) {
        org.springframework.data.domain.Sort sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id");
        if ("price_asc".equalsIgnoreCase(sort)) {
            sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "priceBuy");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "priceBuy");
        } else if ("sold_desc".equalsIgnoreCase(sort)) {
            sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "sold");
        } else if ("rating_desc".equalsIgnoreCase(sort)) {
            sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "rating");
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sortObj);

        if (categoryId != null && categoryId > 0 && keyword != null && !keyword.trim().isEmpty()) {
            return productRepository.searchByCategoryAndKeyword(categoryId, keyword.trim(), pageable);
        } else if (categoryId != null && categoryId > 0) {
            return productRepository.findByCategoryId(categoryId, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword.trim(), keyword.trim(), pageable);
        } else {
            return productRepository.findByIsActiveTrueAndIsSellingTrue(pageable);
        }
    }

    @Override
    public List<Product> getProductsByStore(Long storeId) {
        return productRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    @Override
    @Transactional
    public boolean toggleWishlist(Long userId, Long productId) {
        var existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false;
        } else {
            User user = userRepository.findById(userId).orElseThrow();
            Product product = productRepository.findById(productId).orElseThrow();
            wishlistRepository.save(ProductWishlist.builder().user(user).product(product).build());
            return true;
        }
    }

    @Override
    public boolean isWishlisted(Long userId, Long productId) {
        if (userId == null) return false;
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public List<Product> getWishlist(Long userId) {
        List<ProductWishlist> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return items.stream().map(ProductWishlist::getProduct).toList();
    }

    @Override
    public long getFollowerCount(Long productId) {
        if (productId == null) return 0;
        return wishlistRepository.countByProductId(productId);
    }

    @Override
    @Transactional
    public void recordRecentlyViewed(Long userId, Long productId) {
        if (userId == null) return;
        var existing = recentlyViewedRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            RecentlyViewed rv = existing.get();
            rv.setViewedAt(java.time.LocalDateTime.now());
            recentlyViewedRepository.save(rv);
        } else {
            User user = userRepository.findById(userId).orElse(null);
            Product product = productRepository.findById(productId).orElse(null);
            if (user != null && product != null) {
                recentlyViewedRepository.save(RecentlyViewed.builder().user(user).product(product).build());
            }
        }
    }

    @Override
    public List<Product> getRecentlyViewed(Long userId) {
        if (userId == null) return List.of();
        List<RecentlyViewed> list = recentlyViewedRepository.findTop10ByUserIdOrderByViewedAtDesc(userId);
        return list.stream().map(RecentlyViewed::getProduct).toList();
    }

    @Override
    @Transactional
    public Product createProduct(ProductForm form) throws IOException {
        // Kiểm tra danh mục
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));

        // Chống bán lỗ: Nếu giá bán < giá nhập mà chưa xác nhận bán lỗ thì ném lỗi
        if (form.getPriceBuy().compareTo(form.getImportPrice()) < 0 && !form.isConfirmSellAtLoss()) {
            throw new IllegalArgumentException("WARN_SELL_AT_LOSS");
        }

        // 1. Lưu thông tin sản phẩm
        Product product = Product.builder()
                .category(category)
                .name(form.getName())
                .description(form.getDescription())
                .importPrice(form.getImportPrice())
                .priceBuy(form.getPriceBuy())
                .priceRentPerDay(java.math.BigDecimal.ZERO)
                .depositFee(java.math.BigDecimal.ZERO)
                .isActive(true)
                .isSelling(true)
                .sold(0)
                .rating(5.0)
                .build();
        Product savedProduct = productRepository.save(product);

        // 2. Upload danh sách ảnh lên Cloudinary
        if (form.getImageFiles() != null && !form.getImageFiles().isEmpty()) {
            boolean isFirst = true;
            for (MultipartFile file : form.getImageFiles()) {
                if (!file.isEmpty()) {
                    String imageUrl = cloudinaryService.uploadImage(file);
                    ProductImage productImage = ProductImage.builder()
                            .product(savedProduct)
                            .imageUrl(imageUrl)
                            .isMain(isFirst)
                            .build();
                    productImageRepository.save(productImage);
                    if (isFirst) {
                        savedProduct.setImageUrl(imageUrl);
                        productRepository.save(savedProduct);
                    }
                    isFirst = false;
                }
            }
        }

        // 3. Tự động khởi tạo tồn kho tại tất cả chi nhánh
        List<Store> activeStores = storeRepository.findByStatusTrue();
        for (Store store : activeStores) {
            StoreInventory inventory = StoreInventory.builder()
                    .store(store)
                    .product(savedProduct)
                    .quantity(form.getInitialStock())
                    .build();
            storeInventoryRepository.save(inventory);
        }

        return savedProduct;
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sản phẩm không tồn tại");
        }
        productRepository.deleteById(id);
    }
}
