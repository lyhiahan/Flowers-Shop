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
                .priceRentPerDay(form.getPriceRentPerDay())
                .depositFee(form.getDepositFee())
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
                            .isMain(isFirst) // Ảnh đầu tiên làm ảnh chính
                            .build();
                    productImageRepository.save(productImage);
                    isFirst = false;
                }
            }
        }

        // 3. Tự động khởi tạo tồn kho 0 tại tất cả chi nhánh
        List<Store> activeStores = storeRepository.findByStatusTrue();
        for (Store store : activeStores) {
            StoreInventory inventory = StoreInventory.builder()
                    .store(store)
                    .product(savedProduct)
                    .quantity(0)
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