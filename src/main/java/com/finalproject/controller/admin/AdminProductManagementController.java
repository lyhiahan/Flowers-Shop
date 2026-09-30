package com.finalproject.controller.admin;

import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/products-manage")
@RequiredArgsConstructor
public class AdminProductManagementController {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public static String detectStyle(String name) {
        if (name == null) return "Bó hoa";
        String n = name.trim().toLowerCase();
        if (n.startsWith("giỏ")) return "Giỏ hoa";
        if (n.startsWith("hộp")) return "Hộp hoa";
        if (n.startsWith("kệ")) return "Kệ hoa";
        if (n.startsWith("chậu")) return "Chậu hoa";
        if (n.startsWith("lọ") || n.startsWith("bình")) return "Bình hoa";
        return "Bó hoa";
    }

    @GetMapping
    public String listProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
            Model model) {

        List<Product> allProducts = productRepository.findAll();

        // Tự động bảo toàn dữ liệu cho sản phẩm nếu có trường rỗng
        for (Product p : allProducts) {
            boolean modified = false;
            if (p.getPriceBuy() == null) {
                p.setPriceBuy(new BigDecimal("750000"));
                modified = true;
            }
            if (p.getSold() == null) {
                p.setSold(0);
                modified = true;
            }
            if (p.getIsActive() == null) {
                p.setIsActive(true);
                modified = true;
            }
            if (p.getIsSelling() == null) {
                p.setIsSelling(true);
                modified = true;
            }
            if (modified) {
                productRepository.save(p);
            }
        }

        // Tính số lượng cho từng tab kiểm duyệt
        long countAll = allProducts.size();
        long countApproved = allProducts.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).count();
        long countPending = allProducts.stream().filter(p -> p.getIsActive() == null).count();
        long countRejected = allProducts.stream().filter(p -> Boolean.FALSE.equals(p.getIsActive())).count();

        // Lọc theo Tab kiểm duyệt
        List<Product> filtered = allProducts;
        if ("APPROVED".equalsIgnoreCase(tab)) {
            filtered = filtered.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList();
        } else if ("PENDING".equalsIgnoreCase(tab)) {
            filtered = filtered.stream().filter(p -> p.getIsActive() == null).toList();
        } else if ("REJECTED".equalsIgnoreCase(tab)) {
            filtered = filtered.stream().filter(p -> Boolean.FALSE.equals(p.getIsActive())).toList();
        }

        // Lọc theo từ khóa
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase().replace("#", "");
            filtered = filtered.stream().filter(p ->
                    (p.getName() != null && p.getName().toLowerCase().contains(kw))
                    || String.valueOf(p.getId()).contains(kw)
            ).toList();
        }

        // Lọc theo Chi nhánh
        if (storeId != null && storeId > 0) {
            filtered = filtered.stream().filter(p ->
                    p.getStore() != null && storeId.equals(p.getStore().getId())
            ).toList();
        }

        // Lọc theo Kiểu dáng
        if (style != null && !style.trim().isEmpty() && !"ALL".equalsIgnoreCase(style)) {
            filtered = filtered.stream().filter(p ->
                    detectStyle(p.getName()).equalsIgnoreCase(style.trim())
            ).toList();
        }

        // Lọc theo Trạng thái kinh doanh / kiểm duyệt
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            if ("SELLING".equalsIgnoreCase(status)) {
                filtered = filtered.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive()) && Boolean.TRUE.equals(p.getIsSelling())).toList();
            } else if ("PAUSED".equalsIgnoreCase(status)) {
                filtered = filtered.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive()) && !Boolean.TRUE.equals(p.getIsSelling())).toList();
            } else if ("REJECTED".equalsIgnoreCase(status)) {
                filtered = filtered.stream().filter(p -> !Boolean.TRUE.equals(p.getIsActive())).toList();
            }
        }

        List<Store> stores = storeRepository.findAll();

        model.addAttribute("products", filtered);
        model.addAttribute("stores", stores);
        model.addAttribute("countAll", countAll);
        model.addAttribute("countApproved", countApproved);
        model.addAttribute("countPending", countPending);
        model.addAttribute("countRejected", countRejected);

        model.addAttribute("keyword", keyword);
        model.addAttribute("currentStoreId", storeId);
        model.addAttribute("currentStyle", style != null ? style : "ALL");
        model.addAttribute("currentStatus", status != null ? status : "ALL");
        model.addAttribute("currentTab", tab != null ? tab.toUpperCase() : "ALL");

        return "admin/products-manage";
    }

    @PostMapping("/toggle-active/{id}")
    public String toggleProductActive(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        boolean newActive = !Boolean.TRUE.equals(product.getIsActive());
        product.setIsActive(newActive);
        productRepository.save(product);

        if (newActive) {
            ra.addFlashAttribute("successMessage", "Đã phê duyệt sản phẩm [" + product.getName() + "]. Sản phẩm đã đủ điều kiện kinh doanh.");
        } else {
            ra.addFlashAttribute("successMessage", "Đã từ chối / khóa sản phẩm [" + product.getName() + "]. Sản phẩm sẽ không được bán trên hệ thống.");
        }
        return "redirect:/admin/products-manage";
    }

    @PostMapping("/toggle-selling/{id}")
    public String toggleProductSelling(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        boolean newSelling = !Boolean.TRUE.equals(product.getIsSelling());
        product.setIsSelling(newSelling);
        productRepository.save(product);

        if (newSelling) {
            ra.addFlashAttribute("successMessage", "Đã mở bán lại sản phẩm [" + product.getName() + "].");
        } else {
            ra.addFlashAttribute("successMessage", "Đã tạm ngừng bán sản phẩm [" + product.getName() + "].");
        }
        return "redirect:/admin/products-manage";
    }
}
