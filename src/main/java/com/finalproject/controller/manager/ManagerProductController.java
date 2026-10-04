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
    @org.springframework.transaction.annotation.Transactional
    public String addProduct(Principal principal,
                             @RequestParam("name") String name,
                             @RequestParam("categoryId") Long categoryId,
                             @RequestParam("priceBuy") BigDecimal priceBuy,
                             @RequestParam(value = "promotionalPrice", required = false) BigDecimal promotionalPrice,
                             @RequestParam(value = "initialStock", defaultValue = "0") int initialStock,
                             @RequestParam(value = "imageUrl", required = false) String imageUrl,
                             @RequestParam(value = "description", required = false) String description,
                             RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = storeAccess.assignedStore();
        Category category = categoryRepository.findById(categoryId).orElseThrow();

        validatePrice(priceBuy, promotionalPrice);
        validateName(name);
        if (initialStock < 0 || initialStock > 1000000) throw new IllegalArgumentException("Tồn kho phải từ 0 đến 1.000.000.");
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
        inventoryService.setStock(product.getId(), initialStock);
        ra.addFlashAttribute("successMessage", "Thêm mẫu hoa [" + name + "] thành công!");
        return "redirect:/manager/products";
    }

    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable("id") Long id,
                                @RequestParam("name") String name,
                                @RequestParam("categoryId") Long categoryId,
                                @RequestParam("priceBuy") BigDecimal priceBuy,
                                @RequestParam(value = "promotionalPrice", required = false) BigDecimal promotionalPrice,
                                @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes ra) {
        Product p = productRepository.findById(id).orElseThrow();
        storeAccess.requireManager(p.getStore());
        Category cat = categoryRepository.findById(categoryId).orElseThrow();
        validatePrice(priceBuy, promotionalPrice);
        validateName(name);
        p.setName(name);
        p.setCategory(cat);
        p.setPriceBuy(priceBuy);
        p.setPromotionalPrice(promotionalPrice != null ? promotionalPrice : priceBuy);
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            p.setImageUrl(imageUrl.trim());
        }
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
