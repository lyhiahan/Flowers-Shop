package com.finalproject.controller.admin;

import com.finalproject.dto.request.ProductForm;
import com.finalproject.entity.Category;
import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.service.ProductService;
import com.finalproject.service.ProductSeederService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping({"/admin/products", "/admin/products-manage"})
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;
    private final ProductSeederService productSeederService;

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

    /**
     * Xem danh sách sản phẩm trong hệ thống với bộ lọc, tab phân loại và trạng thái cấp phép (UC_028)
     */
    @GetMapping
    public String listProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
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
                if (p.getImportPrice() == null && p.getPriceBuy() != null) {
                    p.setImportPrice(p.getPriceBuy().multiply(new BigDecimal("0.6")).setScale(0, java.math.RoundingMode.HALF_UP));
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

            // Tính số lượng cho từng tab kiểm duyệt / cấp phép
            long countAll = allProducts.size();
            long countApproved = allProducts.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).count();
            long countPending = allProducts.stream().filter(p -> p.getIsActive() == null).count();
            long countBanned = allProducts.stream().filter(p -> Boolean.FALSE.equals(p.getIsActive())).count();

            // Lọc theo Tab kiểm duyệt / cấp phép (UC_028: Licensed products vs Unlicensed products)
            List<Product> filtered = allProducts;
            if ("LICENSED".equalsIgnoreCase(tab) || "APPROVED".equalsIgnoreCase(tab)) {
                filtered = filtered.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList();
            } else if ("UNLICENSED".equalsIgnoreCase(tab) || "BANNED".equalsIgnoreCase(tab) || "REJECTED".equalsIgnoreCase(tab)) {
                filtered = filtered.stream().filter(p -> !Boolean.TRUE.equals(p.getIsActive())).toList();
            } else if ("PENDING".equalsIgnoreCase(tab)) {
                filtered = filtered.stream().filter(p -> p.getIsActive() == null).toList();
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

            // Lọc theo Trạng thái kinh doanh / cấp phép
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                if ("APPROVED".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status) || "LICENSED".equalsIgnoreCase(status)) {
                    filtered = filtered.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList();
                } else if ("SELLING".equalsIgnoreCase(status)) {
                    filtered = filtered.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive()) && Boolean.TRUE.equals(p.getIsSelling())).toList();
                } else if ("PAUSED".equalsIgnoreCase(status)) {
                    filtered = filtered.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive()) && !Boolean.TRUE.equals(p.getIsSelling())).toList();
                } else if ("BANNED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "UNLICENSED".equalsIgnoreCase(status)) {
                    filtered = filtered.stream().filter(p -> !Boolean.TRUE.equals(p.getIsActive())).toList();
                }
            }

            // Phân trang sản phẩm (UC_028)
            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            List<Product> pagedProducts = filtered.subList(fromIndex, toIndex);

            List<Store> stores = storeRepository.findAll();

            model.addAttribute("products", pagedProducts);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("stores", stores);
            model.addAttribute("countAll", countAll);
            model.addAttribute("countApproved", countApproved);
            model.addAttribute("countPending", countPending);
            model.addAttribute("countBanned", countBanned);
            model.addAttribute("countRejected", countBanned); // alias

            model.addAttribute("keyword", keyword);
            model.addAttribute("currentStoreId", storeId);
            model.addAttribute("currentStyle", style != null ? style : "ALL");
            model.addAttribute("currentStatus", status != null ? status : "ALL");
            model.addAttribute("currentTab", tab != null ? tab.toUpperCase() : "ALL");
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("dataLoadError", false);

        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách sản phẩm. Vui lòng thử lại!");
            model.addAttribute("products", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
            model.addAttribute("stores", java.util.Collections.emptyList());
            model.addAttribute("countAll", 0L);
            model.addAttribute("countApproved", 0L);
            model.addAttribute("countPending", 0L);
            model.addAttribute("countBanned", 0L);
            model.addAttribute("countRejected", 0L);
            model.addAttribute("currentTab", tab != null ? tab.toUpperCase() : "ALL");
        }

        return "admin/products-manage";
    }

    /**
     * Cấp phép / Duyệt bán sản phẩm trong toàn hệ thống
     */
    @PostMapping("/approve/{id}")
    public String approveProduct(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        product.setIsActive(true);
        productRepository.save(product);

        ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP / DUYỆT BÁN cho sản phẩm [" + product.getName() + "]. Sản phẩm hiện đã đủ điều kiện kinh doanh trên toàn hệ thống.");
        return "redirect:/admin/products-manage";
    }

    /**
     * Cấm / Thu hồi giấy phép kinh doanh sản phẩm trong toàn hệ thống
     */
    @PostMapping("/ban/{id}")
    public String banProduct(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        product.setIsActive(false);
        productRepository.save(product);

        ra.addFlashAttribute("successMessage", "Đã CẤM / THU HỒI giấy phép của sản phẩm [" + product.getName() + "]. Sản phẩm đã bị ngừng bán trên toàn hệ thống.");
        return "redirect:/admin/products-manage";
    }

    /**
     * Chuyển đổi trạng thái cấp phép (tương thích ngược)
     */
    @PostMapping("/toggle-active/{id}")
    public String toggleProductActive(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        boolean newActive = !Boolean.TRUE.equals(product.getIsActive());
        product.setIsActive(newActive);
        productRepository.save(product);

        if (newActive) {
            ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP cho sản phẩm [" + product.getName() + "]. Sản phẩm đã đủ điều kiện kinh doanh.");
        } else {
            ra.addFlashAttribute("successMessage", "Đã CẤM / THU HỒI giấy phép của sản phẩm [" + product.getName() + "].");
        }
        return "redirect:/admin/products-manage";
    }

    /**
     * Tạm dừng hoặc mở bán lại sản phẩm
     */
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

    /**
     * Cập nhật thông tin và hình ảnh sản phẩm (Dành cho Admin)
     */
    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable("id") Long id,
                                @RequestParam("name") String name,
                                @RequestParam("categoryId") Long categoryId,
                                @RequestParam("priceBuy") java.math.BigDecimal priceBuy,
                                @RequestParam(value = "promotionalPrice", required = false) java.math.BigDecimal promotionalPrice,
                                @RequestParam(value = "importPrice", required = false) java.math.BigDecimal importPrice,
                                @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes ra) {
        Product p = productRepository.findById(id).orElseThrow();
        Category cat = categoryRepository.findById(categoryId).orElseThrow();
        p.setName(name);
        p.setCategory(cat);
        p.setPriceBuy(priceBuy);
        p.setPromotionalPrice(promotionalPrice != null ? promotionalPrice : priceBuy);
        if (importPrice != null) {
            p.setImportPrice(importPrice);
        }
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            p.setImageUrl(imageUrl.trim());
        }
        p.setDescription(description);
        productRepository.save(p);
        ra.addFlashAttribute("successMessage", "Cập nhật thông tin sản phẩm [" + name + "] thành công!");
        return "redirect:/admin/products-manage";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new ProductForm());
        }
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/create")
    public String handleCreateProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        model.addAttribute("categories", categoryRepository.findAll());

        if (bindingResult.hasErrors()) {
            return "admin/product-form";
        }

        try {
            productService.createProduct(form);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm mới thành công!");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException ex) {
            if ("WARN_SELL_AT_LOSS".equals(ex.getMessage())) {
                model.addAttribute("showLossWarningModal", true);
                model.addAttribute("importPriceVal", form.getImportPrice());
                model.addAttribute("priceBuyVal", form.getPriceBuy());
                return "admin/product-form";
            }
            bindingResult.rejectValue("name", "error.product", ex.getMessage());
            return "admin/product-form";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Lỗi khi lưu sản phẩm: " + ex.getMessage());
            return "admin/product-form";
        }
    }

    @PostMapping({"/seed-defaults", "/reset-defaults"})
    public String seedDefaultProducts(org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        int added = productSeederService.seedMissingProductsForStores();
        if (added > 0) {
            ra.addFlashAttribute("successMessage", "Đã bổ sung thành công " + added + " mặt hàng hoa tươi mới (đầy đủ 9 loại sản phẩm và 6 kiểu dáng) cho các chi nhánh!");
        } else {
            ra.addFlashAttribute("successMessage", "Tất cả các chi nhánh hiện đã có đầy đủ 9 loại mặt hàng hoa tươi phong phú!");
        }
        return "redirect:/admin/products-manage";
    }

}
