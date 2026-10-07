package com.finalproject.controller.manager;

import com.finalproject.entity.Category;
import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.UserRepository;
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
    private final com.finalproject.service.CategoryService categoryService;
    private final com.finalproject.service.ProductAttributeService attributes;
    private final UserRepository userRepository;
    private final com.finalproject.service.StoreAccessService storeAccess;
    private final com.finalproject.service.CloudinaryService cloudinaryService;
    private final com.finalproject.repository.ProductImageRepository productImages;

    @GetMapping("/create")
    public String createForm(Model model) {
        storeAccess.requireManager(storeAccess.assignedStore());
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findActiveCategories());
        return "manager/product-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).orElseThrow();
        storeAccess.requireManager(product.getStore());
        if (Boolean.TRUE.equals(product.getIsDeleted())) throw new IllegalArgumentException("Sản phẩm đã bị xóa.");
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findActiveCategories());
        return "manager/product-form";
    }

    @GetMapping
    public String listProducts(Principal principal, Model model,
            @RequestParam(value="tab", defaultValue="selling") String tab,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="8") int size) {
        userRepository.findByUsername(principal.getName()).orElseThrow();
        Long storeId = storeAccess.assignedStore().getId();

        if (!java.util.Set.of("selling", "stored").contains(tab)) throw new IllegalArgumentException("Tab sản phẩm không hợp lệ.");
        var result = com.finalproject.utils.StorePages.load(page, size, pageable -> productRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("store").get("id"), storeId), cb.isFalse(root.get("isDeleted")),
                cb.equal(root.get("isSelling"), "selling".equals(tab))), pageable));
        List<Product> products = result.getContent();
        com.finalproject.utils.StorePages.model(model, result);
        model.addAttribute("activeTab", tab);


        model.addAttribute("products", products);
        java.util.Map<Long, Integer> stock = new java.util.HashMap<>();
        inventories.findByStoreIdAndProductIdIn(storeId, products.stream().map(Product::getId).toList()).forEach(item -> stock.put(item.getProduct().getId(), item.getQuantity()));
        model.addAttribute("stock", stock);
        model.addAttribute("categories", categoryRepository.findActiveCategories());
        model.addAttribute("store", storeAccess.assignedStore());
        return "manager/products";
    }

    @PostMapping("/add")
    @org.springframework.transaction.annotation.Transactional
    public String addProduct(Principal principal,
                             @RequestParam("name") String name,
                             @RequestParam("categoryId") Long categoryId,
                             @RequestParam("priceBuy") BigDecimal priceBuy,
                             @RequestParam(value = "promotionalPrice", required = false) BigDecimal promotionalPrice,
                             @RequestParam(value = "initialStock", defaultValue = "0") int initialStock,
                             @RequestParam(value = "imageUrl", required = false) String imageUrl,
                             @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "styleValueIds", required = false) java.util.List<Long> styleValueIds,
                             @RequestParam(value = "attributesSubmitted", defaultValue = "false") boolean attributesSubmitted,
                             RedirectAttributes ra) {
        userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = storeAccess.assignedStore();
        storeAccess.requireManager(store);
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        if (!categoryService.visible(category)) throw new IllegalArgumentException("Danh mục hoặc danh mục cha đã bị ẩn.");

        validatePrice(priceBuy, promotionalPrice);
        validateName(name);
        if (imageUrl != null && !imageUrl.isBlank() && !com.finalproject.utils.ImageUrls.valid(imageUrl)) throw new IllegalArgumentException("URL ảnh không hợp lệ.");
        if (description != null && description.length() > 10000) throw new IllegalArgumentException("Mô tả tối đa 10000 ký tự.");
        if (initialStock < 0 || initialStock > 1000000) throw new IllegalArgumentException("Tồn kho phải từ 0 đến 1.000.000.");
        var uploaded = uploadProductImage(imageFile);
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
                .imageUrl(uploaded != null ? uploaded.secureUrl() : (imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl.trim() : "/images/flower-logo.png"))
                .description(description)
                .isActive(true)
                .isSelling(true)
                .build();

        if (attributesSubmitted) attributes.assign(product, styleValueIds, true);
        productRepository.save(product);
        saveUploadedImage(product, uploaded);
        inventoryService.setStock(product.getId(), initialStock);
        ra.addFlashAttribute("successMessage", "Thêm mẫu hoa [" + name + "] thành công!");
        return "redirect:/manager/products";
    }

    @PostMapping("/update/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String updateProduct(@PathVariable("id") Long id,
                                @RequestParam("name") String name,
                                @RequestParam("categoryId") Long categoryId,
                                @RequestParam("priceBuy") BigDecimal priceBuy,
                                @RequestParam(value = "promotionalPrice", required = false) BigDecimal promotionalPrice,
                                @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam(value = "styleValueIds", required = false) java.util.List<Long> styleValueIds,
                                @RequestParam(value = "attributesSubmitted", defaultValue = "false") boolean attributesSubmitted,
                                RedirectAttributes ra) {
        Product p = productRepository.findById(id).orElseThrow();
        if (Boolean.TRUE.equals(p.getIsDeleted())) throw new IllegalArgumentException("Sản phẩm đã bị xóa.");
        storeAccess.requireManager(p.getStore());
        Category cat = categoryRepository.findById(categoryId).orElseThrow();
        if (!categoryService.visible(cat)) throw new IllegalArgumentException("Danh mục hoặc danh mục cha đã bị ẩn.");
        validatePrice(priceBuy, promotionalPrice);
        validateName(name);
        if (imageUrl != null && !imageUrl.isBlank() && !com.finalproject.utils.ImageUrls.valid(imageUrl)) throw new IllegalArgumentException("URL ảnh không hợp lệ.");
        if (description != null && description.length() > 10000) throw new IllegalArgumentException("Mô tả tối đa 10000 ký tự.");
        var uploaded = uploadProductImage(imageFile);
        p.setName(name);
        p.setCategory(cat);
        if (attributesSubmitted) attributes.assign(p, styleValueIds, true);
        p.setPriceBuy(priceBuy);
        p.setPromotionalPrice(promotionalPrice != null ? promotionalPrice : priceBuy);
        if (uploaded != null) {
            p.setImageUrl(uploaded.secureUrl());
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            p.setImageUrl(imageUrl.trim());
        }
        p.setDescription(description);
        productRepository.save(p);
        saveUploadedImage(p, uploaded);
        ra.addFlashAttribute("successMessage", "Cập nhật mẫu hoa [" + name + "] thành công!");
        return "redirect:/manager/products";
    }

    @PostMapping("/toggle-selling/{id}")
    public String toggleSelling(@PathVariable("id") Long id, @RequestParam(value="selling", required=false) Boolean selling, RedirectAttributes ra) {
        Product p = productRepository.findById(id).orElseThrow();
        storeAccess.requireManager(p.getStore());
        if (Boolean.TRUE.equals(p.getIsDeleted())) throw new IllegalArgumentException("Sản phẩm đã bị xóa.");
        p.setIsSelling(selling != null ? selling : !Boolean.TRUE.equals(p.getIsSelling()));
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

    private com.finalproject.service.CloudinaryService.UploadResult uploadProductImage(org.springframework.web.multipart.MultipartFile file) {
        try {
            return cloudinaryService.upload(file, "rental_shop_products");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Không thể tải ảnh lên Cloudinary. Vui lòng thử lại.", ex);
        }
    }

    private void saveUploadedImage(Product product, com.finalproject.service.CloudinaryService.UploadResult uploaded) {
        if (uploaded == null) return;
        var previous = productImages.findByProductId(product.getId());
        previous.forEach(image -> image.setIsMain(false));
        productImages.saveAll(previous);
        productImages.save(com.finalproject.entity.ProductImage.builder().product(product).imageUrl(uploaded.secureUrl())
                .cloudinaryPublicId(uploaded.publicId()).isMain(true).build());
    }

    private void validatePrice(BigDecimal price, BigDecimal promotion) {
        if (price == null || price.signum() <= 0 || price.precision() - price.scale() > 16 || price.stripTrailingZeros().scale() > 2
                || (promotion != null && (promotion.signum() <= 0 || promotion.compareTo(price) > 0 || promotion.stripTrailingZeros().scale() > 2))) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0, tối đa 16 chữ số và 2 số thập phân; giá khuyến mãi không vượt quá giá bán.");
        }
    }
    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 200) throw new IllegalArgumentException("Tên sản phẩm phải có từ 1 đến 200 ký tự.");
    }
}
