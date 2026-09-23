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
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @GetMapping
    public String listProducts(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Long storeId = user.getStore() != null ? user.getStore().getId() : 1L;

        List<Product> products = productRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        if (products.isEmpty()) {
            products = productRepository.findAll();
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("store", user.getStore());
        return "manager/products";
    }

    @PostMapping("/add")
    public String addProduct(Principal principal,
                             @RequestParam("name") String name,
                             @RequestParam("categoryId") Long categoryId,
                             @RequestParam("priceBuy") BigDecimal priceBuy,
                             @RequestParam(value = "promotionalPrice", required = false) BigDecimal promotionalPrice,
                             @RequestParam(value = "sold", defaultValue = "0") int sold,
                             @RequestParam(value = "imageUrl", required = false) String imageUrl,
                             @RequestParam(value = "description", required = false) String description,
                             RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = user.getStore() != null ? user.getStore() : storeRepository.findById(1L).orElse(null);
        Category category = categoryRepository.findById(categoryId).orElseThrow();

        Product product = Product.builder()
                .name(name)
                .category(category)
                .store(store)
                .priceBuy(priceBuy)
                .importPrice(priceBuy.multiply(BigDecimal.valueOf(0.6)))
                .priceRentPerDay(priceBuy.multiply(BigDecimal.valueOf(0.1)))
                .depositFee(priceBuy.multiply(BigDecimal.valueOf(0.3)))
                .promotionalPrice(promotionalPrice != null ? promotionalPrice : priceBuy)
                .sold(sold)
                .rating(5.0)
                .imageUrl(imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl.trim() : "https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=600")
                .description(description)
                .isActive(true)
                .isSelling(true)
                .build();

        productRepository.save(product);
        ra.addFlashAttribute("successMessage", "Thêm mẫu hoa [" + name + "] thành công!");
        return "redirect:/manager/products";
    }

    @PostMapping("/toggle-selling/{id}")
    public String toggleSelling(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setIsSelling(!p.getIsSelling());
        productRepository.save(p);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái mở bán sản phẩm!");
        return "redirect:/manager/products";
    }
}
