package com.finalproject.controller.manager;

import com.finalproject.entity.Category;
import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping({"/manager/products", "/vendor/products"})
@RequiredArgsConstructor
public class ManagerProductController {

    private final ProductRepository productRepository;
    private final com.finalproject.repository.StoreInventoryRepository inventories;
    private final com.finalproject.service.InventoryService inventoryService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final com.finalproject.service.StoreAccessService storeAccess;
    private final StoreRepository storeRepository;
    private final com.finalproject.service.ManagedImageUploadService imageUploads;
    private final com.finalproject.repository.ProductImageRepository productImages;

    @GetMapping
    public String listProducts(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Long storeId = storeAccess.assignedStore().getId();

        List<Product> products = productRepository.findByStoreIdOrderByCreatedAtDesc(storeId);


        model.addAttribute("products", products);
        java.util.Map<Long, Integer> stock = new java.util.HashMap<>();
        for (Product product : products) stock.put(product.getId(), inventories.findByStoreIdAndProductId(storeId, product.getId())
                .map(com.finalproject.entity.StoreInventory::getQuantity).orElse(0));
        model.addAttribute("stock", stock);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("store", user.getStore());
        return "manager/products";
    }

    @PostMapping("/add")
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String addProduct(Principal principal,
                             @RequestParam("name") String name,
                             @RequestParam("categoryId") Long categoryId,
                             @RequestParam("priceBuy") BigDecimal priceBuy,
                             @RequestParam(value = "promotionalPrice", required = false) BigDecimal promotionalPrice,
                             @RequestParam(value = "initialStock", defaultValue = "0") int initialStock,
                             @RequestParam(value = "imageUrl", required = false) String imageUrl,
                             @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                             @RequestParam(value = "description", required = false) String description,
                             RedirectAttributes ra) throws java.io.IOException {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = storeAccess.assignedStore();
        Category category = categoryRepository.findById(categoryId).orElseThrow();

        validatePrice(priceBuy, promotionalPrice);
        if (initialStock < 0 || initialStock > 1000000) throw new IllegalArgumentException("Tồn kho không hợp lệ.");
        Product product = Product.builder()
                .name(name)
                .category(category)
                .store(store)
                .priceBuy(priceBuy)
                .importPrice(priceBuy.multiply(BigDecimal.valueOf(0.6)))
                .priceRentPerDay(priceBuy.multiply(BigDecimal.valueOf(0.1)))
                .depositFee(priceBuy.multiply(BigDecimal.valueOf(0.3)))
                .promotionalPrice(promotionalPrice != null ? promotionalPrice : priceBuy)
                .sold(0)
                .rating(5.0)
                .imageUrl(imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl.trim() : "https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=600")
                .description(description)
                .isActive(true)
                .isSelling(true)
                .build();

        productRepository.save(product);
        updateImage(product, imageFile, imageUrl);
        inventoryService.setStock(product.getId(), initialStock);
        ra.addFlashAttribute("successMessage", "Thêm mẫu hoa [" + name + "] thành công!");
        return "redirect:/manager/products";
    }

    @PostMapping("/update/{id}")
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String updateProduct(@PathVariable("id") Long id,
                                @RequestParam("name") String name,
                                @RequestParam("categoryId") Long categoryId,
                                @RequestParam("priceBuy") BigDecimal priceBuy,
                                @RequestParam(value = "promotionalPrice", required = false) BigDecimal promotionalPrice,
                                @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes ra) throws java.io.IOException {
        Product p = productRepository.findById(id).orElseThrow();
        storeAccess.requireManager(p.getStore());
        Category cat = categoryRepository.findById(categoryId).orElseThrow();
        validatePrice(priceBuy, promotionalPrice);
        p.setName(name);
        p.setCategory(cat);
        p.setPriceBuy(priceBuy);
        p.setPromotionalPrice(promotionalPrice != null ? promotionalPrice : priceBuy);
        updateImage(p, imageFile, imageUrl);
        p.setDescription(description);
        productRepository.save(p);
        ra.addFlashAttribute("successMessage", "Cập nhật mẫu hoa [" + name + "] thành công!");
        return "redirect:/manager/products";
    }

    @PostMapping("/toggle-selling/{id}")
    public String toggleSelling(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product p = productRepository.findById(id).orElseThrow();
        storeAccess.requireManager(p.getStore());
        p.setIsSelling(!p.getIsSelling());
        productRepository.save(p);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái mở bán sản phẩm!");
        return "redirect:/manager/products";
    }
    @PostMapping("/stock/{id}")
    public String updateStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity, RedirectAttributes ra) {
        inventoryService.setStock(id, quantity);
        ra.addFlashAttribute("successMessage", "Đã cập nhật tồn kho thực tế.");
        return "redirect:/manager/products";
    }

    private void updateImage(Product product, org.springframework.web.multipart.MultipartFile file, String url) throws java.io.IOException {
        var uploaded = imageUploads.upload(file, "rental_shop_products");
        String newUrl = uploaded != null ? uploaded.secureUrl() : url != null ? url.trim() : "";
        if (newUrl.isBlank()) return;
        var main = productImages.findByProductIdAndIsMainTrue(product.getId())
                .orElseGet(() -> com.finalproject.entity.ProductImage.builder().product(product).isMain(true).build());
        if (uploaded != null || !newUrl.equals(main.getImageUrl())) {
            imageUploads.deleteAfterCommit(main.getCloudinaryPublicId());
            main.setCloudinaryPublicId(uploaded != null ? uploaded.publicId() : null);
        }
        main.setImageUrl(newUrl);
        productImages.save(main);
        product.setImageUrl(newUrl);
    }

    private void validatePrice(BigDecimal price, BigDecimal promotion) {
        if (price == null || price.signum() < 0 || (promotion != null && (promotion.signum() < 0 || promotion.compareTo(price) > 0))) {
            throw new IllegalArgumentException("Giá bán phải không âm; giá khuyến mãi không vượt quá giá bán.");
        }
    }
}
