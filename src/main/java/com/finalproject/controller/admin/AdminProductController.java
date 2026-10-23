package com.finalproject.controller.admin;

import com.finalproject.dto.request.ProductForm;
import com.finalproject.entity.Product;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.service.ProductService;
import com.finalproject.service.ProductSeederService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping({"/admin/products", "/admin/products-manage"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;
    private final ProductSeederService productSeederService;
    private final com.finalproject.repository.StoreInventoryRepository inventories;
    private final com.finalproject.service.InventoryService inventoryService;

    public record BranchStock(Long storeId, String storeName, int quantity, boolean archived) {}

    private void addBranchStocks(Product product, Model model) {
        var stockByStore = new java.util.HashMap<Long, com.finalproject.entity.StoreInventory>();
        inventories.findByProductId(product.getId()).forEach(row ->
                stockByStore.put(row.getStore().getId(), row));
        model.addAttribute("stockRows", storeRepository.findAll().stream()
                .filter(store -> com.finalproject.service.ProductAvailability.supportsBranch(product, store))
                .map(store -> {
                    var row = stockByStore.get(store.getId());
                    return new BranchStock(store.getId(), store.getName(),
                            row == null || row.getQuantity() == null ? 0 : row.getQuantity(),
                            row != null && Boolean.TRUE.equals(row.getIsDeleted()));
                }).toList());
    }



    private String safeReturnUrl(String value) {
        if (value != null && !value.contains("\\") && value.chars().noneMatch(Character::isISOControl)) {
            try {
                java.net.URI uri = java.net.URI.create(value);
                if (uri.getScheme() == null && uri.getRawAuthority() == null && uri.getFragment() == null
                        && List.of("/admin/products", "/admin/products-manage").contains(uri.getPath())) return value;
            } catch (IllegalArgumentException ignored) {}
        }
        return "/admin/products-manage";
    }

    private String buildReturnUrl(String returnUrl, String tab, String keyword, String style, String status, Integer page, Integer size) {
        if (com.finalproject.utils.SafeRedirect.isWithin(returnUrl, "/admin/products-manage", "/admin/products")) {
            return returnUrl;
        }
        org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder.fromPath("/admin/products-manage");
        if (tab != null && !tab.isBlank()) builder.queryParam("tab", tab);
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        if (style != null && !style.isBlank()) builder.queryParam("style", style);
        if (status != null && !status.isBlank()) builder.queryParam("status", status);
        if (page != null) builder.queryParam("page", page);
        if (size != null) builder.queryParam("size", size);
        return builder.toUriString();
    }

    /**
     * Cấp phép / Duyệt bán sản phẩm trong toàn hệ thống
     */
    @PostMapping("/approve/{id}")
    public String approveProduct(@PathVariable("id") Long id,
                                 @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                 RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        product.setIsActive(true);
        productRepository.save(product);

        ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP / DUYỆT BÁN cho sản phẩm [" + product.getName() + "]. Sản phẩm hiện đã đủ điều kiện kinh doanh trên toàn hệ thống.");
        ra.addFlashAttribute("highlightProductId", id);
        String target = safeReturnUrl(returnUrl);
        return "redirect:" + target;
    }

    /**
     * Cấm / Thu hồi giấy phép kinh doanh sản phẩm trong toàn hệ thống
     */
    @PostMapping("/ban/{id}")
    public String banProduct(@PathVariable("id") Long id,
                             @RequestParam(value = "returnUrl", required = false) String returnUrl,
                             RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        product.setIsActive(false);
        productRepository.save(product);

        ra.addFlashAttribute("successMessage", "Đã CẤM / THU HỒI giấy phép của sản phẩm [" + product.getName() + "]. Sản phẩm đã bị ngừng bán trên toàn hệ thống.");
        ra.addFlashAttribute("highlightProductId", id);
        String target = safeReturnUrl(returnUrl);
        return "redirect:" + target;
    }

    /**
     * Chuyển đổi trạng thái cấp phép (tương thích ngược)
     */
    @PostMapping("/toggle-active/{id}")
    public String toggleProductActive(@PathVariable("id") Long id,
                                      @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                      RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        boolean newActive = !Boolean.TRUE.equals(product.getIsActive());
        product.setIsActive(newActive);
        productRepository.save(product);

        if (newActive) {
            ra.addFlashAttribute("successMessage", "Đã CẤP PHÉP cho sản phẩm [" + product.getName() + "]. Sản phẩm đã đủ điều kiện kinh doanh.");
        } else {
            ra.addFlashAttribute("successMessage", "Đã CẤM / THU HỒI giấy phép của sản phẩm [" + product.getName() + "].");
        }
        ra.addFlashAttribute("highlightProductId", id);
        String target = safeReturnUrl(returnUrl);
        return "redirect:" + target;
    }

    /**
     * Tạm dừng hoặc mở bán lại sản phẩm
     */
    @PostMapping("/toggle-selling/{id}")
    public String toggleProductSelling(@PathVariable("id") Long id,
                                        @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                        RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElseThrow();
        boolean newSelling = !Boolean.TRUE.equals(product.getIsSelling());
        product.setIsSelling(newSelling);
        productRepository.save(product);

        if (newSelling) {
            ra.addFlashAttribute("successMessage", "Đã mở bán lại sản phẩm [" + product.getName() + "].");
        } else {
            ra.addFlashAttribute("successMessage", "Đã tạm ngừng bán sản phẩm [" + product.getName() + "].");
        }
        ra.addFlashAttribute("highlightProductId", id);
        String target = safeReturnUrl(returnUrl);
        return "redirect:" + target;
    }

    @GetMapping("/create")
    public String showCreateForm(
            @RequestParam(value = "returnUrl", required = false) String returnUrl,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "tab", required = false) String tab,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "size", required = false) Integer size,
            Model model) {
        if (!model.containsAttribute("productForm")) {
            ProductForm form = new ProductForm();
            form.setReturnUrl(buildReturnUrl(returnUrl, tab, keyword, style, status, page, size));
            model.addAttribute("productForm", form);
        }
        model.addAttribute("isEdit", false);
        model.addAttribute("categories", categoryRepository.findActiveCategories());
        model.addAttribute("stores", storeRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/create")
    public String handleCreateProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        model.addAttribute("isEdit", false);
        model.addAttribute("categories", categoryRepository.findActiveCategories());
        model.addAttribute("stores", storeRepository.findAll());

        if (bindingResult.hasErrors()) {
            return "admin/product-form";
        }

        try {
            Product created = productService.createProduct(form);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm mới [" + form.getName() + "] thành công!");
            if (created != null && created.getId() != null) {
                redirectAttributes.addFlashAttribute("highlightProductId", created.getId());
            }
            String target = safeReturnUrl(form.getReturnUrl());
            return "redirect:" + target;
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

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id,
                               @RequestParam(value = "returnUrl", required = false) String returnUrl,
                               @RequestParam(value = "page", required = false) Integer page,
                               @RequestParam(value = "tab", required = false) String tab,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "style", required = false) String style,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "size", required = false) Integer size,
                               Model model,
                               RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null || Boolean.TRUE.equals(product.getIsDeleted())) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm hoặc sản phẩm đã bị xóa!");
            return "redirect:/admin/products-manage";
        }

        ProductForm form = new ProductForm();
        form.setId(product.getId());
        form.setName(product.getName());
        form.setStyleValueIds(product.getSelectedStyleValueIds());
        form.setFlowerTypes(product.attributeLabel("Loại hoa"));
        form.setFlowerColors(product.attributeLabel("Màu sắc"));
        form.setFlowerStyles(product.attributeLabel("Phong cách"));
        form.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        form.setAdditionalCategoryIds(product.getAdditionalCategories().stream().map(com.finalproject.entity.Category::getId).toList());
        form.setDescription(product.getDescription());
        form.setImportPrice(product.getImportPrice());
        form.setPriceBuy(product.getPriceBuy());
        form.setStoreId(product.getStore() != null ? product.getStore().getId() : null);
        form.setIsSelling(product.getIsSelling() != null ? product.getIsSelling() : true);
        form.setIsActive(product.getIsActive() != null ? product.getIsActive() : true);
        form.setImageUrl(product.getImageUrl());
        form.setSold(product.getSold() != null ? product.getSold() : 0);
        form.setReturnUrl(buildReturnUrl(returnUrl, tab, keyword, style, status, page, size));

        model.addAttribute("productForm", form);
        model.addAttribute("isEdit", true);
        model.addAttribute("categories", categoryRepository.findActiveCategories());
        model.addAttribute("stores", storeRepository.findAll());
        addBranchStocks(product, model);
        return "admin/product-form";
    }

    @PostMapping("/edit/{id}")
    public String handleEditProduct(@PathVariable("id") Long id,
                                    @Valid @ModelAttribute("productForm") ProductForm form,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        model.addAttribute("isEdit", true);
        model.addAttribute("categories", categoryRepository.findActiveCategories());
        model.addAttribute("stores", storeRepository.findAll());
        productRepository.findById(id).ifPresent(product -> addBranchStocks(product, model));

        if (bindingResult.hasErrors()) {
            return "admin/product-form";
        }

        try {
            productService.updateProduct(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm [" + form.getName() + "] thành công!");
            redirectAttributes.addFlashAttribute("highlightProductId", id);
            String target = safeReturnUrl(form.getReturnUrl());
            return "redirect:" + target;
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
            model.addAttribute("errorMessage", "Lỗi khi cập nhật sản phẩm: " + ex.getMessage());
            return "admin/product-form";
        }
    }

    @PostMapping("/restock/{id}")
    public String restock(@PathVariable Long id,
                          @RequestParam("storeId") Long storeId,
                          @RequestParam("addedQuantity") String addedQuantity,
                          @RequestParam(value = "returnUrl", required = false) String returnUrl,
                          RedirectAttributes ra) {
        try {
            int amount = Integer.parseInt(addedQuantity.trim());
            int remaining = inventoryService.restock(id, storeId, amount);
            ra.addFlashAttribute("successMessage", "Đã nhập thêm " + amount
                    + " sản phẩm. Tồn kho chi nhánh hiện là " + remaining + ".");
        } catch (NumberFormatException ex) {
            ra.addFlashAttribute("errorMessage", "Số lượng nhập thêm phải là số nguyên dương.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        ra.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        return "redirect:/admin/products-manage/edit/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id,
                                @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            try {
                productService.softDeleteProduct(id);
                ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm [" + product.getName() + "] khỏi danh sách bán. Có thể khôi phục về Ngừng bán tại mục Đã xóa.");
            } catch (IllegalArgumentException ex) {
                ra.addFlashAttribute("errorMessage", ex.getMessage());
            }
        } else {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm cần xóa.");
        }
        String target = safeReturnUrl(returnUrl);
        return "redirect:" + target;
    }

    @PostMapping("/restore/{id}")
    public String restoreProduct(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            productService.restoreProduct(id);
            ra.addFlashAttribute("successMessage", "Đã khôi phục sản phẩm về Ngừng bán. Kiểm tra thông tin và tồn kho trước khi mở bán lại.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products-manage?status=PAUSED";
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
