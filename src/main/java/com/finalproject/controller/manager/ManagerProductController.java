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
    private final com.finalproject.service.StoreProductService storeProducts;

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
            @RequestParam(value="keyword", required=false) String keyword,
            @RequestParam(value="categoryId", required=false) Long categoryId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="8") int size) {
        userRepository.findByUsername(principal.getName()).orElseThrow();
        Long storeId = storeAccess.assignedStore().getId();

        if (!java.util.Set.of("selling", "stored", "all", "deleted").contains(tab)) throw new IllegalArgumentException("Tab sản phẩm không hợp lệ.");
        var result = com.finalproject.utils.StorePages.load(page, size, pageable -> productRepository.findAll(
                com.finalproject.service.StoreProductService.catalog(storeId, tab, keyword, categoryId), pageable));
        List<Product> products = result.getContent();
        com.finalproject.utils.StorePages.model(model, result);
        model.addAttribute("activeTab", tab);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);

        // Counts for tab badges
        model.addAttribute("countSelling", productRepository.count(com.finalproject.service.StoreProductService.catalog(storeId, "selling")));
        model.addAttribute("countStored", productRepository.count(com.finalproject.service.StoreProductService.catalog(storeId, "stored")));
        model.addAttribute("countAll", productRepository.count(com.finalproject.service.StoreProductService.catalog(storeId, "all")));
        model.addAttribute("countDeleted", productRepository.count(com.finalproject.service.StoreProductService.catalog(storeId, "deleted")));

        model.addAttribute("products", products);
        java.util.Map<Long, Integer> stock = new java.util.HashMap<>();
        java.util.Map<Long, Boolean> selling = new java.util.HashMap<>();
        inventories.findByStoreIdAndProductIdIn(storeId, products.stream().map(Product::getId).toList()).forEach(item -> {
            stock.put(item.getProduct().getId(), item.getQuantity()); selling.put(item.getProduct().getId(), item.isAvailableForSale());
        });
        products.forEach(p -> selling.putIfAbsent(p.getId(), com.finalproject.service.ProductAvailability.isPublic(p)));
        model.addAttribute("branchSelling", selling);
        model.addAttribute("currentStockQuantity", inventories.sumRemainingQuantity(storeId));
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
                             @RequestParam(value = "additionalCategoryIds", required = false) List<Long> additionalCategoryIds,
                             @RequestParam(value = "categoriesSubmitted", defaultValue = "false") boolean categoriesSubmitted,
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

        categoryService.assignAdditional(product, additionalCategoryIds, categoriesSubmitted);
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
                                @RequestParam(value = "additionalCategoryIds", required = false) List<Long> additionalCategoryIds,
                                @RequestParam(value = "categoriesSubmitted", defaultValue = "false") boolean categoriesSubmitted,
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
        categoryService.assignAdditional(p, additionalCategoryIds, categoriesSubmitted);
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
        Product p = storeProducts.requireSelectedProduct(id);
        var local = inventories.findByStoreIdAndProductId(storeAccess.assignedStore().getId(), id);
        boolean currentlySelling = local.map(com.finalproject.entity.StoreInventory::isAvailableForSale)
                .orElse(com.finalproject.service.ProductAvailability.isPublic(p));
        storeProducts.setSelling(id, selling != null ? selling : !currentlySelling);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái mở bán sản phẩm!");
        return "redirect:/manager/products";
    }

    @GetMapping("/branch/{id}")
    public String editShared(@PathVariable Long id, Model model) {
        var product = storeProducts.requireSelectedProduct(id);
        if (product.getStore() != null) return "redirect:/manager/products/edit/" + id;
        var inventory = inventories.findByStoreIdAndProductId(storeAccess.assignedStore().getId(), id).orElseThrow();
        if (Boolean.TRUE.equals(inventory.getIsDeleted()) || Boolean.TRUE.equals(product.getIsDeleted()))
            throw new IllegalArgumentException("Khôi phục sản phẩm trước khi chỉnh sửa.");
        model.addAttribute("product", product); model.addAttribute("inventory", inventory);
        return "manager/branch-product-form";
    }

    @PostMapping("/branch/{id}")
    public String updateShared(@PathVariable Long id, @RequestParam int quantity,
            @RequestParam(defaultValue="false") boolean selling, RedirectAttributes ra) {
        storeProducts.updateShared(id, quantity, selling);
        ra.addFlashAttribute("successMessage", "Đã cập nhật sản phẩm chung tại chi nhánh này.");
        return "redirect:/manager/products";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            storeProducts.delete(id);
            ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi danh mục chi nhánh. Tồn kho và lịch sử đơn được giữ lại; có thể khôi phục tại mục Đã xóa.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/products";
    }

    @PostMapping("/restore/{id}")
    public String restore(@PathVariable Long id, RedirectAttributes ra) {
        storeProducts.restore(id);
        ra.addFlashAttribute("successMessage", "Đã khôi phục sản phẩm về mục Ngừng bán. Kiểm tra thông tin và tồn kho trước khi mở bán lại.");
        return "redirect:/manager/products?tab=stored";
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
        com.finalproject.utils.ProductPrices.validate(price, promotion);
    }
    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 200) throw new IllegalArgumentException("Tên sản phẩm phải có từ 1 đến 200 ký tự.");
    }
}
