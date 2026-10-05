package com.finalproject.controller.admin;

import com.finalproject.dto.request.ProductForm;
import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
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

import java.math.BigDecimal;
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
    private final com.finalproject.repository.StoreInventoryRepository storeInventoryRepository;
    private final com.finalproject.repository.OrderDetailRepository orderDetailRepository;

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

    public static String detectStyle(String name) {
        if (name == null) return "Bó hoa";
        String n = name.trim().toLowerCase();
        if (n.startsWith("giỏ") || n.contains("giỏ")) return "Giỏ hoa";
        if (n.startsWith("hộp") || n.contains("hộp")) return "Hộp hoa";
        if (n.startsWith("kệ") || n.contains("kệ")) return "Kệ hoa";
        if (n.startsWith("chậu") || n.contains("chậu")) return "Chậu hoa";
        if (n.startsWith("lọ") || n.startsWith("bình") || n.contains("bình") || n.contains("lọ")) return "Bình hoa";
        if (n.startsWith("lẵng") || n.contains("lẵng")) return "Lẵng hoa";
        if (n.startsWith("cành") || n.contains("cành")) return "Cành hoa";
        return "Bó hoa";
    }

    public static String detectUnit(String name) {
        if (name == null) return "bó";
        String n = name.trim().toLowerCase();
        if (n.startsWith("giỏ") || n.contains("giỏ")) return "giỏ";
        if (n.startsWith("hộp") || n.contains("hộp")) return "hộp";
        if (n.startsWith("chậu") || n.contains("chậu")) return "chậu";
        if (n.startsWith("kệ") || n.contains("kệ")) return "kệ";
        if (n.startsWith("lọ") || n.startsWith("bình") || n.contains("bình") || n.contains("lọ")) return "bình";
        if (n.startsWith("lẵng") || n.contains("lẵng")) return "lẵng";
        if (n.startsWith("cành") || n.contains("cành")) return "cành";
        return "bó";
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
            @RequestParam(value = "stockStatus", required = false, defaultValue = "ALL") String stockStatus,
            @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            List<Product> allProducts = productRepository.findAll().stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                    .toList();

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

            List<Store> stores = storeRepository.findAll();

            // 1. Tải bản đồ tồn kho thực tế từ store_inventories theo sản phẩm và chi nhánh
            java.util.Map<Long, java.util.Map<Long, Integer>> productStoreStockMap = new java.util.HashMap<>();
            try {
                var allInventories = storeInventoryRepository.findAll();
                for (var inv : allInventories) {
                    if (inv.getProduct() != null && inv.getStore() != null) {
                        productStoreStockMap
                                .computeIfAbsent(inv.getProduct().getId(), k -> new java.util.HashMap<>())
                                .put(inv.getStore().getId(), inv.getQuantity() != null ? inv.getQuantity() : 0);
                    }
                }
            } catch (Exception ignored) {}

            // 2. Tải bản đồ số lượng đang giữ (Reserved) cho các đơn hàng đang xử lý
            java.util.Map<Long, java.util.Map<Long, Integer>> productStoreReservedMap = new java.util.HashMap<>();
            try {
                List<Object[]> reservedList = orderDetailRepository.findReservedStockGrouped();
                if (reservedList != null) {
                    for (Object[] row : reservedList) {
                        Long pId = (Long) row[0];
                        Long sId = (Long) row[1];
                        Number qty = (Number) row[2];
                        if (pId != null && sId != null && qty != null) {
                            productStoreReservedMap
                                    .computeIfAbsent(pId, k -> new java.util.HashMap<>())
                                    .put(sId, qty.intValue());
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 3. Xây dựng DTO tổng hợp tồn kho và tồn kho hiển thị cho từng sản phẩm
            java.util.Map<Long, com.finalproject.dto.response.ProductInventorySummaryDto> inventorySummaryMap = new java.util.HashMap<>();
            java.util.Map<Long, Integer> stockMap = new java.util.HashMap<>();

            for (Product p : allProducts) {
                String unit = detectUnit(p.getName());
                java.util.List<com.finalproject.dto.response.ProductInventorySummaryDto.BranchInventoryDto> branchStocks = new java.util.ArrayList<>();
                int totalAvail = 0;
                int totalPhys = 0;
                int totalRes = 0;

                for (Store s : stores) {
                    int avail = productStoreStockMap.getOrDefault(p.getId(), java.util.Collections.emptyMap()).getOrDefault(s.getId(), 0);
                    int res = productStoreReservedMap.getOrDefault(p.getId(), java.util.Collections.emptyMap()).getOrDefault(s.getId(), 0);
                    int phys = avail + res;
                    totalAvail += avail;
                    totalPhys += phys;
                    totalRes += res;
                    branchStocks.add(com.finalproject.dto.response.ProductInventorySummaryDto.BranchInventoryDto.builder()
                            .storeId(s.getId())
                            .storeName(s.getName())
                            .storeAddress(s.getAddress())
                            .physicalStock(phys)
                            .reservedStock(res)
                            .availableStock(avail)
                            .unit(unit)
                            .build());
                }

                int displayStock = (storeId != null && storeId > 0)
                        ? productStoreStockMap.getOrDefault(p.getId(), java.util.Collections.emptyMap()).getOrDefault(storeId, 0)
                        : totalAvail;
                stockMap.put(p.getId(), displayStock);

                String stockStatusType;
                String stockStatusText;
                if (displayStock <= 0) {
                    stockStatusType = "OUT_OF_STOCK";
                    stockStatusText = "Hết hàng";
                } else if (displayStock <= 5) {
                    stockStatusType = "LOW_STOCK";
                    stockStatusText = "Sắp hết";
                } else {
                    stockStatusType = "IN_STOCK";
                    stockStatusText = "Còn hàng";
                }

                com.finalproject.dto.response.ProductInventorySummaryDto summary = com.finalproject.dto.response.ProductInventorySummaryDto.builder()
                        .productId(p.getId())
                        .productName(p.getName())
                        .unit(unit)
                        .totalAvailable(totalAvail)
                        .totalPhysical(totalPhys)
                        .totalReserved(totalRes)
                        .status(stockStatusType)
                        .statusLabel(stockStatusText)
                        .branchStocks(branchStocks)
                        .build();
                inventorySummaryMap.put(p.getId(), summary);
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
                        (p.getStore() != null && storeId.equals(p.getStore().getId()))
                        || (productStoreStockMap.containsKey(p.getId()) && productStoreStockMap.get(p.getId()).containsKey(storeId))
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

            // Lọc theo Tình trạng tồn kho (Tất cả / Còn hàng / Sắp hết / Hết hàng)
            if (stockStatus != null && !stockStatus.trim().isEmpty() && !"ALL".equalsIgnoreCase(stockStatus)) {
                if ("IN_STOCK".equalsIgnoreCase(stockStatus)) {
                    filtered = filtered.stream().filter(p -> stockMap.getOrDefault(p.getId(), 0) > 5).toList();
                } else if ("LOW_STOCK".equalsIgnoreCase(stockStatus)) {
                    filtered = filtered.stream().filter(p -> {
                        int s = stockMap.getOrDefault(p.getId(), 0);
                        return s > 0 && s <= 5;
                    }).toList();
                } else if ("OUT_OF_STOCK".equalsIgnoreCase(stockStatus)) {
                    filtered = filtered.stream().filter(p -> stockMap.getOrDefault(p.getId(), 0) <= 0).toList();
                }
            }

            // Phân trang sản phẩm (UC_028)
            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            // Nếu page vượt quá số trang hiện có (ví dụ vừa xóa sản phẩm cuối cùng của trang) thì tự động lùi về trang cuối
            if (page >= totalPages) {
                page = Math.max(0, totalPages - 1);
            }
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            List<Product> pagedProducts = filtered.subList(fromIndex, toIndex);

            model.addAttribute("products", pagedProducts);
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
            model.addAttribute("currentStockStatus", stockStatus != null ? stockStatus.toUpperCase() : "ALL");
            model.addAttribute("currentTab", tab != null ? tab.toUpperCase() : "ALL");
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("dataLoadError", false);

            model.addAttribute("inventorySummaryMap", inventorySummaryMap);
            model.addAttribute("stockMap", stockMap);
            model.addAttribute("stockColumnTitle", (storeId != null && storeId > 0) ? "Tồn kho" : "Tổng tồn kho");

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
            model.addAttribute("inventorySummaryMap", java.util.Collections.emptyMap());
            model.addAttribute("stockMap", java.util.Collections.emptyMap());
            model.addAttribute("currentStockStatus", "ALL");
            model.addAttribute("stockColumnTitle", "Tổng tồn kho");
        }

        return "admin/products-manage";
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
        form.setStyleValueIds(product.getStyleValues().stream().map(com.finalproject.entity.StyleValue::getId).toList());
        form.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
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

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id,
                                @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            productService.softDeleteProduct(id);
            ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm [" + product.getName() + "] khỏi danh sách bán. Toàn bộ lịch sử và dữ liệu đơn hàng liên quan được bảo toàn nguyên vẹn.");
        } else {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm cần xóa.");
        }
        String target = safeReturnUrl(returnUrl);
        return "redirect:" + target;
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
