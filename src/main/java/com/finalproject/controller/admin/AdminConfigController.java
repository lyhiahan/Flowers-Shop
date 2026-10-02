package com.finalproject.controller.admin;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminConfigController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CommissionRepository commissionRepository;
    private final DeliveryRepository deliveryRepository;
    private final VoucherRepository voucherRepository;
    private final UserLevelRepository userLevelRepository;
    private final StoreLevelRepository storeLevelRepository;
    private final StyleRepository styleRepository;
    private final StyleValueRepository styleValueRepository;
    private final StoreRepository storeRepository;

    // --- 1. Quản lý Loại sản phẩm / Danh mục (Categories - UC_030) ---
    @GetMapping({"/categories", "/category", "/loai-san-pham"})
    public String listCategories(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<Category> allCategories = categoryRepository.findAll();

            java.util.List<Category> filtered = allCategories;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(c ->
                        (c.getName() != null && c.getName().toLowerCase().contains(kw))
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(kw))
                        || String.valueOf(c.getId()).contains(kw)
                ).toList();
            }

            // Phân trang danh sách loại sản phẩm (Bước 4)
            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<Category> pagedCategories = filtered.subList(fromIndex, toIndex);

            // Cây danh mục (Bước 5: Review cây category)
            java.util.List<Category> rootCategories = allCategories.stream()
                    .filter(c -> c.getParent() == null)
                    .toList();

            model.addAttribute("categories", pagedCategories);
            model.addAttribute("allCategories", allCategories);
            model.addAttribute("rootCategories", rootCategories);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);

        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách loại sản phẩm. Vui lòng thử lại!");
            model.addAttribute("categories", java.util.Collections.emptyList());
            model.addAttribute("allCategories", java.util.Collections.emptyList());
            model.addAttribute("rootCategories", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/categories";
    }

    /**
     * Bước 6 & 7: Chuyển đến trang form tạo category mới
     */
    @GetMapping("/categories/create")
    public String createCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("isEdit", false);
        return "admin/category-form";
    }

    /**
     * Alternative 6a & 7a: Chuyển đến trang form chỉnh sửa category
     */
    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable("id") Long id, Model model) {
        Category cat = categoryRepository.findById(id).orElse(null);
        if (cat == null) {
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", cat);
        model.addAttribute("allCategories", categoryRepository.findAll().stream().filter(c -> !c.getId().equals(id)).toList());
        model.addAttribute("isEdit", true);
        return "admin/category-form";
    }

    /**
     * Bước 8 - 12: Thêm mới category
     */
    @PostMapping("/categories/add")
    public String addCategory(@RequestParam("name") String name,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "parentId", required = false) Long parentId,
                              RedirectAttributes ra) {
        try {
            Category parent = null;
            if (parentId != null && parentId > 0) {
                parent = categoryRepository.findById(parentId).orElse(null);
            }
            Category cat = Category.builder()
                    .name(name)
                    .description(description)
                    .parent(parent)
                    .isDeleted(false)
                    .build();
            categoryRepository.save(cat);
            ra.addFlashAttribute("successMessage", "Thêm mới loại sản phẩm [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể thêm loại sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /**
     * Cập nhật category
     */
    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable("id") Long id,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "parentId", required = false) Long parentId,
                                 RedirectAttributes ra) {
        try {
            Category cat = categoryRepository.findById(id).orElseThrow();
            cat.setName(name);
            cat.setDescription(description);
            if (parentId != null && parentId > 0 && !parentId.equals(id)) {
                cat.setParent(categoryRepository.findById(parentId).orElse(null));
            } else if (parentId != null && parentId == 0) {
                cat.setParent(null);
            }
            categoryRepository.save(cat);
            ra.addFlashAttribute("successMessage", "Cập nhật loại sản phẩm [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể cập nhật loại sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /**
     * Alternative 6-9b: Xóa category (đánh dấu xóa mềm isDeleted = true)
     */
    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Category cat = categoryRepository.findById(id).orElse(null);
            if (cat == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy loại sản phẩm cần xóa!");
                return "redirect:/admin/categories";
            }
            String catName = cat.getName();
            cat.setIsDeleted(true);
            categoryRepository.save(cat);
            ra.addFlashAttribute("successMessage", "Đã xóa loại sản phẩm [" + catName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa loại sản phẩm này: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /**
     * Alternative 6-9c: Khôi phục category (isDeleted = false)
     */
    @PostMapping("/categories/restore/{id}")
    public String restoreCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Category cat = categoryRepository.findById(id).orElse(null);
            if (cat == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy loại sản phẩm cần khôi phục!");
                return "redirect:/admin/categories";
            }
            String catName = cat.getName();
            cat.setIsDeleted(false);
            categoryRepository.save(cat);
            ra.addFlashAttribute("successMessage", "Khôi phục loại sản phẩm [" + catName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể khôi phục loại sản phẩm này: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/reset-defaults")
    public String resetDefaultCategories(RedirectAttributes ra) {
        record DefCat(String name, String desc) {}
        java.util.List<DefCat> defaults = java.util.List.of(
            new DefCat("Hoa Sinh Nhật", "Các mẫu bó hoa, lẵng hoa chúc mừng sinh nhật ngọt ngào, ý nghĩa"),
            new DefCat("Hoa Khai Trương", "Kệ hoa, lẵng hoa khai trương sang trọng phát tài phát lộc"),
            new DefCat("Hoa Tình Yêu & Valentine", "Bó hoa hồng đỏ Ecuador, hoa baby trắng lãng mạn tỏ tình"),
            new DefCat("Lan Hồ Điệp Sang Trọng", "Chậu hoa lan hồ điệp cao cấp trưng Tết, biếu sếp, mừng tân gia"),
            new DefCat("Hoa Cưới Cầm Tay Cô Dâu", "Bó hoa cưới tinh tế tôn vinh vẻ đẹp cô dâu trong ngày trọng đại"),
            new DefCat("Hoa Chúc Mừng & Tốt Nghiệp", "Thiết kế tươi sáng, gửi gắm lời chúc may mắn thành công"),
            new DefCat("Hoa Chia Buồn & Kính Viếng", "Kệ hoa trang trọng, thành kính phân ưu cùng gia quyến"),
            new DefCat("Bình Hoa Nghệ Thuật Để Bàn", "Bình gốm, bình thủy tinh cắm hoa tươi trang trí phòng khách và văn phòng"),
            new DefCat("Hộp Hoa & Giỏ Hoa Quà Tặng", "Hộp hoa mica, hộp tròn cao cấp và giỏ hoa tươi thiết kế độc đáo")
        );
        for (DefCat d : defaults) {
            java.util.Optional<Category> existing = categoryRepository.findByName(d.name());
            if (existing.isEmpty()) {
                categoryRepository.save(Category.builder().name(d.name()).description(d.desc()).isDeleted(false).build());
            } else if (Boolean.TRUE.equals(existing.get().getIsDeleted())) {
                Category c = existing.get();
                c.setIsDeleted(false);
                categoryRepository.save(c);
            }
        }
        ra.addFlashAttribute("successMessage", "Khôi phục toàn bộ danh sách 9 loại sản phẩm hoa tươi mẫu thành công!");
        return "redirect:/admin/categories";
    }

    // --- 2. Quản lý Chính sách hoa hồng (Commissions - UC_035) ---
    @GetMapping({"/commissions", "/commission", "/chinh-sach-hoa-hong"})
    public String listCommissions(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<Commission> allCommissions = commissionRepository.findAllByOrderByIdAsc();
            java.util.List<Commission> filtered = allCommissions;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(c ->
                        (c.getName() != null && c.getName().toLowerCase().contains(kw))
                        || String.valueOf(c.getId()).contains(kw)
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(kw))
                ).toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<Commission> pagedCommissions = filtered.subList(fromIndex, toIndex);

            model.addAttribute("commissions", pagedCommissions);
            model.addAttribute("allCommissions", allCommissions);
            model.addAttribute("stores", storeRepository.findAll());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách chính sách hoa hồng. Vui lòng thử lại!");
            model.addAttribute("commissions", java.util.Collections.emptyList());
            model.addAttribute("allCommissions", java.util.Collections.emptyList());
            model.addAttribute("stores", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/commissions";
    }

    /**
     * Bước 5 & 6: Chọn nút New commission để thêm mới -> Hiển thị form thêm mới
     */
    @GetMapping("/commissions/create")
    public String createCommissionForm(Model model) {
        model.addAttribute("commission", Commission.builder().cost(BigDecimal.TEN).isDeleted(false).build());
        model.addAttribute("isEdit", false);
        return "admin/commission-form";
    }

    /**
     * Alternative 5a & 6a: Chọn nút Edit để chỉnh sửa -> Hiển thị form chỉnh sửa
     */
    @GetMapping("/commissions/edit/{id}")
    public String editCommissionForm(@PathVariable("id") Long id, Model model) {
        Commission c = commissionRepository.findById(id).orElse(null);
        if (c == null) {
            return "redirect:/admin/commissions";
        }
        model.addAttribute("commission", c);
        model.addAttribute("isEdit", true);
        return "admin/commission-form";
    }

    /**
     * Bước 7 - 11: Nhập đầy đủ thông tin, Submit, Confirm -> Thêm mới thành công
     */
    @PostMapping("/commissions/add")
    public String addCommission(@RequestParam("name") String name,
                                @RequestParam(value = "cost", defaultValue = "0") BigDecimal cost,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes ra) {
        try {
            String cleanName = name != null ? name.trim() : "";
            if (cleanName.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng nhập tên chính sách hoa hồng!");
                return "redirect:/admin/commissions";
            }
            Commission c = Commission.builder()
                    .name(cleanName)
                    .cost(cost != null ? cost : BigDecimal.ZERO)
                    .description(description)
                    .isDeleted(false)
                    .build();
            commissionRepository.save(c);
            ra.addFlashAttribute("successMessage", "Thêm chính sách hoa hồng [" + cleanName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi thêm chính sách hoa hồng: " + ex.getMessage());
        }
        return "redirect:/admin/commissions";
    }

    /**
     * Alternative 5a: Cập nhật chính sách hoa hồng
     */
    @RequestMapping(value = "/commissions/update/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String updateCommission(@PathVariable("id") Long id,
                                   @RequestParam("name") String name,
                                   @RequestParam(value = "cost", defaultValue = "0") BigDecimal cost,
                                   @RequestParam(value = "description", required = false) String description,
                                   @RequestParam(value = "isDeleted", required = false) Boolean isDeleted,
                                   RedirectAttributes ra) {
        try {
            Commission c = commissionRepository.findById(id).orElse(null);
            if (c == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy chính sách hoa hồng #" + id);
                return "redirect:/admin/commissions";
            }
            String cleanName = name != null ? name.trim() : c.getName();
            c.setName(cleanName);
            if (cost != null) {
                c.setCost(cost);
            }
            c.setDescription(description);
            if (isDeleted != null) {
                c.setIsDeleted(isDeleted);
            }
            commissionRepository.save(c);
            ra.addFlashAttribute("successMessage", "Cập nhật chính sách hoa hồng [" + cleanName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi cập nhật chính sách hoa hồng: " + ex.getMessage());
        }
        return "redirect:/admin/commissions";
    }

    /**
     * Alternative 5-8b: Xóa chính sách hoa hồng (soft delete)
     */
    @RequestMapping(value = "/commissions/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteCommission(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Commission c = commissionRepository.findById(id).orElseThrow();
            c.setIsDeleted(true);
            commissionRepository.save(c);
            ra.addFlashAttribute("successMessage", "Xóa chính sách hoa hồng [" + c.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa chính sách hoa hồng: " + ex.getMessage());
        }
        return "redirect:/admin/commissions";
    }

    /**
     * Alternative 5-8c: Khôi phục chính sách hoa hồng
     */
    @PostMapping("/commissions/restore/{id}")
    public String restoreCommission(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Commission c = commissionRepository.findById(id).orElseThrow();
            c.setIsDeleted(false);
            commissionRepository.save(c);
            ra.addFlashAttribute("successMessage", "Khôi phục chính sách hoa hồng [" + c.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi khôi phục chính sách hoa hồng: " + ex.getMessage());
        }
        return "redirect:/admin/commissions";
    }

    @PostMapping("/commissions/reset-defaults")
    public String resetDefaultCommissions(RedirectAttributes ra) {
        record DefComm(String name, BigDecimal cost, String desc) {}
        java.util.List<DefComm> defaults = java.util.List.of(
            new DefComm("Gói Cơ Bản (Standard)", new BigDecimal("10"), "Tỷ lệ chiết khấu sàn 10% áp dụng cho chi nhánh mới gia nhập"),
            new DefComm("Gói Đối Tác Thân Thiết", new BigDecimal("8"), "Tỷ lệ chiết khấu ưu đãi 8% cho cửa hàng có trên 100 đơn/tháng"),
            new DefComm("Gói Đối Tác Chiến Lược", new BigDecimal("5"), "Tỷ lệ chiết khấu đặc biệt 5% cho chuỗi phân phối hoa tươi lớn"),
            new DefComm("Phí Dịch Vụ Cố Định Theo Đơn", new BigDecimal("25000"), "Mức thu cố định 25.000 ₫/đơn cho đơn hoa sự kiện")
        );
        for (DefComm d : defaults) {
            java.util.Optional<Commission> existing = commissionRepository.findAll().stream()
                .filter(x -> x.getName().equalsIgnoreCase(d.name()))
                .findFirst();
            if (existing.isPresent()) {
                Commission c = existing.get();
                c.setCost(d.cost());
                c.setDescription(d.desc());
                c.setIsDeleted(false);
                commissionRepository.save(c);
            } else {
                commissionRepository.save(Commission.builder().name(d.name()).cost(d.cost()).description(d.desc()).isDeleted(false).build());
            }
        }
        ra.addFlashAttribute("successMessage", "Khôi phục toàn bộ danh sách chính sách hoa hồng mẫu thành công!");
        return "redirect:/admin/commissions";
    }

    // --- 3. Quản lý Đơn vị vận chuyển / Nhà vận chuyển (Deliveries - UC_034) ---
    @GetMapping({"/deliveries", "/delivery", "/don-vi-van-chuyen"})
    public String listDeliveries(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<Delivery> allDeliveries = deliveryRepository.findAllByOrderByIdAsc();
            java.util.List<Delivery> filtered = allDeliveries;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(d ->
                        (d.getName() != null && d.getName().toLowerCase().contains(kw))
                        || String.valueOf(d.getId()).contains(kw)
                        || (d.getDescription() != null && d.getDescription().toLowerCase().contains(kw))
                ).toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<Delivery> pagedDeliveries = filtered.subList(fromIndex, toIndex);

            model.addAttribute("deliveries", pagedDeliveries);
            model.addAttribute("allDeliveries", allDeliveries);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách đơn vị vận chuyển. Vui lòng thử lại!");
            model.addAttribute("deliveries", java.util.Collections.emptyList());
            model.addAttribute("allDeliveries", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/deliveries";
    }

    /**
     * Bước 5 & 6: Chọn nút New delivery unit để thêm mới -> Hiển thị form thêm mới
     */
    @GetMapping("/deliveries/create")
    public String createDeliveryForm(Model model) {
        model.addAttribute("delivery", Delivery.builder().price(BigDecimal.ZERO).isActive(true).isDeleted(false).build());
        model.addAttribute("isEdit", false);
        return "admin/delivery-form";
    }

    /**
     * Alternative 5a & 6a: Chọn nút Edit để chỉnh sửa -> Hiển thị form chỉnh sửa
     */
    @GetMapping("/deliveries/edit/{id}")
    public String editDeliveryForm(@PathVariable("id") Long id, Model model) {
        Delivery d = deliveryRepository.findById(id).orElse(null);
        if (d == null) {
            return "redirect:/admin/deliveries";
        }
        model.addAttribute("delivery", d);
        model.addAttribute("isEdit", true);
        return "admin/delivery-form";
    }

    /**
     * Bước 7 - 11: Nhập thông tin, Submit, Confirm -> Thêm mới thành công
     */
    @PostMapping("/deliveries/add")
    public String addDelivery(@RequestParam("name") String name,
                              @RequestParam(value = "price", defaultValue = "0") BigDecimal price,
                              @RequestParam(value = "description", required = false) String description,
                              RedirectAttributes ra) {
        try {
            String cleanName = name != null ? name.trim() : "";
            if (cleanName.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng nhập tên đơn vị vận chuyển!");
                return "redirect:/admin/deliveries";
            }
            Delivery d = Delivery.builder()
                    .name(cleanName)
                    .price(price != null ? price : BigDecimal.ZERO)
                    .description(description)
                    .isActive(true)
                    .isDeleted(false)
                    .build();
            deliveryRepository.save(d);
            ra.addFlashAttribute("successMessage", "Thêm đơn vị vận chuyển [" + cleanName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi thêm đơn vị vận chuyển: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    /**
     * Alternative 5a: Cập nhật đơn vị vận chuyển
     */
    @RequestMapping(value = "/deliveries/update/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String updateDelivery(@PathVariable("id") Long id,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "price", defaultValue = "0") BigDecimal price,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "isActive", required = false) Boolean isActive,
                                 RedirectAttributes ra) {
        try {
            Delivery d = deliveryRepository.findById(id).orElse(null);
            if (d == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy đơn vị vận chuyển #" + id);
                return "redirect:/admin/deliveries";
            }
            String cleanName = name != null ? name.trim() : d.getName();
            d.setName(cleanName);
            if (price != null) {
                d.setPrice(price);
            }
            d.setDescription(description);
            if (isActive != null) {
                d.setIsActive(isActive);
            }
            deliveryRepository.save(d);
            ra.addFlashAttribute("successMessage", "Cập nhật đơn vị vận chuyển [" + cleanName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi cập nhật đơn vị vận chuyển: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/toggle/{id}")
    public String toggleDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        Delivery d = deliveryRepository.findById(id).orElseThrow();
        boolean newActive = !Boolean.TRUE.equals(d.getIsActive());
        d.setIsActive(newActive);
        deliveryRepository.save(d);
        ra.addFlashAttribute("successMessage", "Đã " + (newActive ? "bật áp dụng" : "tạm dừng") + " đơn vị vận chuyển [" + d.getName() + "].");
        return "redirect:/admin/deliveries";
    }

    /**
     * Alternative 5-8b: Xóa đơn vị vận chuyển (soft delete)
     */
    @RequestMapping(value = "/deliveries/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Delivery d = deliveryRepository.findById(id).orElseThrow();
            d.setIsDeleted(true);
            deliveryRepository.save(d);
            ra.addFlashAttribute("successMessage", "Xóa đơn vị vận chuyển [" + d.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa đơn vị vận chuyển: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    /**
     * Alternative 5-8c: Khôi phục đơn vị vận chuyển
     */
    @PostMapping("/deliveries/restore/{id}")
    public String restoreDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Delivery d = deliveryRepository.findById(id).orElseThrow();
            d.setIsDeleted(false);
            deliveryRepository.save(d);
            ra.addFlashAttribute("successMessage", "Khôi phục đơn vị vận chuyển [" + d.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi khôi phục đơn vị vận chuyển: " + ex.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/reset-defaults")
    public String resetDefaultDeliveries(RedirectAttributes ra) {
        record DefDeliv(String name, BigDecimal price, String desc) {}
        java.util.List<DefDeliv> defaults = java.util.List.of(
            new DefDeliv("Giao Hàng Tiêu Chuẩn Trong Ngày", new BigDecimal("25000"), "Giao tận nơi trong vòng 4-6 giờ, đóng gói hoa cẩn thận có giữ ẩm."),
            new DefDeliv("GrabExpress Hỏa Tốc (2H)", new BigDecimal("45000"), "Giao nhanh trong 2 giờ bảo quản hoa tươi trong lồng kính xe máy."),
            new DefDeliv("Ahamove Siêu Tốc Hoa Tươi", new BigDecimal("50000"), "Tài xế chuyên giao hoa tươi và bánh kem, cam kết không dập nát.")
        );
        for (DefDeliv d : defaults) {
            java.util.Optional<Delivery> existing = deliveryRepository.findAll().stream()
                .filter(x -> x.getName().equalsIgnoreCase(d.name()))
                .findFirst();
            if (existing.isPresent()) {
                Delivery del = existing.get();
                del.setPrice(d.price());
                del.setDescription(d.desc());
                del.setIsActive(true);
                del.setIsDeleted(false);
                deliveryRepository.save(del);
            } else {
                deliveryRepository.save(Delivery.builder().name(d.name()).price(d.price()).description(d.desc()).isActive(true).isDeleted(false).build());
            }
        }
        ra.addFlashAttribute("successMessage", "Khôi phục toàn bộ danh sách đơn vị vận chuyển mẫu thành công!");
        return "redirect:/admin/deliveries";
    }

    // --- 4. Quản lý Khuyến mãi toàn sàn (Vouchers) ---
    @GetMapping("/vouchers")
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepository.findByStoreIsNullOrderByCreatedAtDesc());
        return "admin/vouchers";
    }

    @PostMapping("/vouchers/add")
    public String addPlatformVoucher(@RequestParam("code") String code,
                                     @RequestParam("name") String name,
                                     @RequestParam("discountType") String discountType,
                                     @RequestParam("discountValue") BigDecimal discountValue,
                                     @RequestParam(value = "minOrderValue", defaultValue = "0") BigDecimal minOrderValue,
                                     @RequestParam(value = "maxDiscountAmount", required = false) BigDecimal maxDiscountAmount,
                                     @RequestParam(value = "quantity", defaultValue = "100") int quantity,
                                     @RequestParam("startDate") String startDate,
                                     @RequestParam("endDate") String endDate,
                                     RedirectAttributes ra) {
        try {
            // 1. Chuẩn hóa & kiểm tra Mã khuyến mãi
            String cleanCode = code != null ? code.trim().toUpperCase() : "";
            if (cleanCode.length() < 3 || cleanCode.length() > 30 || !cleanCode.matches("^[A-Z0-9_-]+$")) {
                ra.addFlashAttribute("errorMessage", "Mã khuyến mãi không hợp lệ! Vui lòng nhập từ 3 đến 30 ký tự (chỉ gồm chữ cái viết hoa, chữ số, gạch '-' hoặc '_', không có khoảng trắng).");
                return "redirect:/admin/vouchers";
            }
            if (voucherRepository.existsByCode(cleanCode)) {
                ra.addFlashAttribute("errorMessage", "Mã khuyến mãi [" + cleanCode + "] đã tồn tại trong hệ thống! Vui lòng chọn mã khác.");
                return "redirect:/admin/vouchers";
            }

            // 2. Kiểm tra Tên chương trình
            String cleanName = name != null ? name.trim() : "";
            if (cleanName.length() < 3 || cleanName.length() > 150) {
                ra.addFlashAttribute("errorMessage", "Tên chương trình khuyến mãi phải từ 3 đến 150 ký tự!");
                return "redirect:/admin/vouchers";
            }

            // 3. Kiểm tra Loại & Giá trị giảm
            if (!java.util.Set.of("PERCENT", "FIXED_AMOUNT", "FREE_SHIP").contains(discountType)) {
                ra.addFlashAttribute("errorMessage", "Loại giảm giá không hợp lệ!");
                return "redirect:/admin/vouchers";
            }

            if ("PERCENT".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                    ra.addFlashAttribute("errorMessage", "Tỷ lệ giảm giá phần trăm phải lớn hơn 0% và không vượt quá 100%!");
                    return "redirect:/admin/vouchers";
                }
            } else if ("FIXED_AMOUNT".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.valueOf(1000)) < 0) {
                    ra.addFlashAttribute("errorMessage", "Số tiền giảm cố định phải từ 1.000 ₫ trở lên!");
                    return "redirect:/admin/vouchers";
                }
                if (minOrderValue != null && minOrderValue.compareTo(BigDecimal.ZERO) > 0 && discountValue.compareTo(minOrderValue) >= 0) {
                    ra.addFlashAttribute("errorMessage", "Mức giảm cố định (" + String.format("%,.0f", discountValue) + " ₫) không được lớn hơn hoặc bằng giá trị đơn hàng tối thiểu (" + String.format("%,.0f", minOrderValue) + " ₫)!");
                    return "redirect:/admin/vouchers";
                }
            } else if ("FREE_SHIP".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                    discountValue = BigDecimal.valueOf(30000); // Phí ship chuẩn mặc định
                }
            }

            // 4. Kiểm tra Đơn tối thiểu & Giảm tối đa
            if (minOrderValue == null || minOrderValue.compareTo(BigDecimal.ZERO) < 0) {
                ra.addFlashAttribute("errorMessage", "Giá trị đơn hàng tối thiểu không được âm!");
                return "redirect:/admin/vouchers";
            }

            if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
                ra.addFlashAttribute("errorMessage", "Mức giảm tối đa không được âm!");
                return "redirect:/admin/vouchers";
            }

            // 5. Kiểm tra Số lượng phát hành
            if (quantity < 1) {
                ra.addFlashAttribute("errorMessage", "Số lượng mã phát hành phải từ 1 lượt trở lên!");
                return "redirect:/admin/vouchers";
            }

            // 6. Kiểm tra Ngày áp dụng
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (start.isAfter(end)) {
                ra.addFlashAttribute("errorMessage", "Ngày kết thúc (" + end + ") phải sau hoặc cùng ngày với ngày bắt đầu (" + start + ")!");
                return "redirect:/admin/vouchers";
            }

            Voucher v = Voucher.builder()
                    .code(cleanCode)
                    .name(cleanName)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .minOrderValue(minOrderValue)
                    .maxDiscountAmount(maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0 ? maxDiscountAmount : null)
                    .store(null) // Voucher toàn sàn của Admin
                    .quantity(quantity)
                    .usedCount(0)
                    .startDate(start)
                    .endDate(end)
                    .isActive(true)
                    .build();

            voucherRepository.save(v);
            ra.addFlashAttribute("successMessage", "Đã tạo mã khuyến mãi toàn sàn [" + cleanCode + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi tạo mã khuyến mãi: " + ex.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/update/{id}")
    public String updatePlatformVoucher(@PathVariable("id") Long id,
                                        @RequestParam("name") String name,
                                        @RequestParam("discountType") String discountType,
                                        @RequestParam("discountValue") BigDecimal discountValue,
                                        @RequestParam(value = "minOrderValue", defaultValue = "0") BigDecimal minOrderValue,
                                        @RequestParam(value = "maxDiscountAmount", required = false) BigDecimal maxDiscountAmount,
                                        @RequestParam("quantity") int quantity,
                                        @RequestParam("startDate") String startDate,
                                        @RequestParam("endDate") String endDate,
                                        @RequestParam(value = "isActive", defaultValue = "true") boolean isActive,
                                        RedirectAttributes ra) {
        try {
            Voucher v = voucherRepository.findById(id).orElse(null);
            if (v == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy mã khuyến mãi cần sửa!");
                return "redirect:/admin/vouchers";
            }

            // Kiểm tra Tên chương trình
            String cleanName = name != null ? name.trim() : "";
            if (cleanName.length() < 3 || cleanName.length() > 150) {
                ra.addFlashAttribute("errorMessage", "Tên chương trình khuyến mãi phải từ 3 đến 150 ký tự!");
                return "redirect:/admin/vouchers";
            }

            // Kiểm tra Loại & Giá trị giảm
            if (!java.util.Set.of("PERCENT", "FIXED_AMOUNT", "FREE_SHIP").contains(discountType)) {
                ra.addFlashAttribute("errorMessage", "Loại giảm giá không hợp lệ!");
                return "redirect:/admin/vouchers";
            }

            if ("PERCENT".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                    ra.addFlashAttribute("errorMessage", "Tỷ lệ giảm giá phần trăm phải lớn hơn 0% và không vượt quá 100%!");
                    return "redirect:/admin/vouchers";
                }
            } else if ("FIXED_AMOUNT".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.valueOf(1000)) < 0) {
                    ra.addFlashAttribute("errorMessage", "Số tiền giảm cố định phải từ 1.000 ₫ trở lên!");
                    return "redirect:/admin/vouchers";
                }
                if (minOrderValue != null && minOrderValue.compareTo(BigDecimal.ZERO) > 0 && discountValue.compareTo(minOrderValue) >= 0) {
                    ra.addFlashAttribute("errorMessage", "Mức giảm cố định không được lớn hơn hoặc bằng giá trị đơn hàng tối thiểu!");
                    return "redirect:/admin/vouchers";
                }
            } else if ("FREE_SHIP".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                    discountValue = BigDecimal.valueOf(30000);
                }
            }

            // Kiểm tra Đơn tối thiểu
            if (minOrderValue == null || minOrderValue.compareTo(BigDecimal.ZERO) < 0) {
                ra.addFlashAttribute("errorMessage", "Giá trị đơn hàng tối thiểu không được âm!");
                return "redirect:/admin/vouchers";
            }

            // Kiểm tra Số lượng: Không được nhỏ hơn số lượt đã dùng
            if (quantity < v.getUsedCount()) {
                ra.addFlashAttribute("errorMessage", "Số lượng phát hành (" + quantity + ") không được nhỏ hơn số lượt mã đã sử dụng (" + v.getUsedCount() + " lượt)!");
                return "redirect:/admin/vouchers";
            }

            // Kiểm tra Ngày áp dụng
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (start.isAfter(end)) {
                ra.addFlashAttribute("errorMessage", "Ngày kết thúc phải sau hoặc cùng ngày với ngày bắt đầu!");
                return "redirect:/admin/vouchers";
            }

            v.setName(cleanName);
            v.setDiscountType(discountType);
            v.setDiscountValue(discountValue);
            v.setMinOrderValue(minOrderValue);
            v.setMaxDiscountAmount(maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0 ? maxDiscountAmount : null);
            v.setQuantity(quantity);
            v.setStartDate(start);
            v.setEndDate(end);
            v.setIsActive(isActive);

            voucherRepository.save(v);
            ra.addFlashAttribute("successMessage", "Đã cập nhật mã khuyến mãi [" + v.getCode() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi cập nhật mã khuyến mãi: " + ex.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @RequestMapping(value = "/vouchers/toggle/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String togglePlatformVoucher(@PathVariable("id") Long id, RedirectAttributes ra) {
        Voucher v = voucherRepository.findById(id).orElse(null);
        if (v == null) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy mã khuyến mãi!");
            return "redirect:/admin/vouchers";
        }
        boolean newStatus = !Boolean.TRUE.equals(v.getIsActive());
        v.setIsActive(newStatus);
        voucherRepository.save(v);
        ra.addFlashAttribute("successMessage", "Đã " + (newStatus ? "bật kích hoạt" : "tắt") + " mã khuyến mãi [" + v.getCode() + "] thành công!");
        return "redirect:/admin/vouchers";
    }

    @RequestMapping(value = "/vouchers/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deletePlatformVoucher(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Voucher v = voucherRepository.findById(id).orElse(null);
            if (v == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy mã khuyến mãi cần xóa!");
                return "redirect:/admin/vouchers";
            }
            String code = v.getCode();
            if (v.getUsedCount() != null && v.getUsedCount() > 0) {
                // Đã có đơn hàng sử dụng -> tắt trạng thái để bảo vệ tính toàn vẹn dữ liệu
                v.setIsActive(false);
                voucherRepository.save(v);
                ra.addFlashAttribute("successMessage", "Mã khuyến mãi [" + code + "] đã có " + v.getUsedCount() + " lượt sử dụng trong các đơn hàng, hệ thống đã chuyển sang trạng thái [Đã tắt] thay vì xóa để bảo toàn dữ liệu lịch sử.");
            } else {
                // Chưa từng sử dụng -> xóa vĩnh viễn
                voucherRepository.delete(v);
                ra.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn mã khuyến mãi [" + code + "] khỏi hệ thống!");
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa mã khuyến mãi: " + ex.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    // --- 5. Quản lý Cấp độ người dùng (UserLevel - UC_032) ---
    /**
     * Bước 1 - 5: Hiển thị bảng danh sách các user level, phân trang Next/Prev
     */
    @GetMapping({"/user-levels", "/user-level", "/cap-bac-khach-hang"})
    public String listUserLevels(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<UserLevel> allLevels = userLevelRepository.findAllByOrderByMinPointAsc();

            java.util.List<UserLevel> filtered = allLevels;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(ul ->
                        (ul.getName() != null && ul.getName().toLowerCase().contains(kw))
                        || String.valueOf(ul.getId()).contains(kw)
                        || (ul.getMinPoint() != null && String.valueOf(ul.getMinPoint()).contains(kw))
                ).toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<UserLevel> pagedLevels = filtered.subList(fromIndex, toIndex);

            model.addAttribute("levels", pagedLevels);
            model.addAttribute("allLevels", allLevels);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách cấp bậc người dùng. Vui lòng thử lại!");
            model.addAttribute("levels", java.util.Collections.emptyList());
            model.addAttribute("allLevels", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/user-levels";
    }

    /**
     * Bước 6 & 7: Hiển thị form thêm level mới
     */
    @GetMapping("/user-levels/create")
    public String createUserLevelForm(Model model) {
        model.addAttribute("userLevel", UserLevel.builder().minPoint(0).discount(BigDecimal.ZERO).build());
        model.addAttribute("isEdit", false);
        return "admin/user-level-form";
    }

    /**
     * Alternative 6a & 7a: Hiển thị form chỉnh sửa level
     */
    @GetMapping("/user-levels/edit/{id}")
    public String editUserLevelForm(@PathVariable("id") Long id, Model model) {
        UserLevel ul = userLevelRepository.findById(id).orElse(null);
        if (ul == null) {
            return "redirect:/admin/user-levels";
        }
        model.addAttribute("userLevel", ul);
        model.addAttribute("isEdit", true);
        return "admin/user-level-form";
    }

    /**
     * Bước 8 - 12: Thêm mới user level
     */
    @PostMapping("/user-levels/add")
    public String addUserLevel(@RequestParam("name") String name,
                               @RequestParam("minPoint") int minPoint,
                               @RequestParam("discount") BigDecimal discount,
                               RedirectAttributes ra) {
        try {
            UserLevel ul = UserLevel.builder()
                    .name(name)
                    .minPoint(minPoint)
                    .discount(discount)
                    .isDeleted(false)
                    .build();
            userLevelRepository.save(ul);
            ra.addFlashAttribute("successMessage", "Thêm mới cấp bậc khách hàng [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể thêm cấp bậc khách hàng: " + ex.getMessage());
        }
        return "redirect:/admin/user-levels";
    }

    /**
     * Alternative 6a: Cập nhật user level
     */
    @PostMapping("/user-levels/update/{id}")
    public String updateUserLevel(@PathVariable("id") Long id,
                                  @RequestParam("name") String name,
                                  @RequestParam("minPoint") int minPoint,
                                  @RequestParam("discount") BigDecimal discount,
                                  RedirectAttributes ra) {
        try {
            UserLevel ul = userLevelRepository.findById(id).orElseThrow();
            ul.setName(name);
            ul.setMinPoint(minPoint);
            ul.setDiscount(discount);
            userLevelRepository.save(ul);
            ra.addFlashAttribute("successMessage", "Cập nhật cấp bậc khách hàng [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể cập nhật cấp bậc khách hàng: " + ex.getMessage());
        }
        return "redirect:/admin/user-levels";
    }

    /**
     * Alternative 6-10b: Xóa level (isDeleted = true)
     */
    @PostMapping("/user-levels/delete/{id}")
    public String deleteUserLevel(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            UserLevel ul = userLevelRepository.findById(id).orElseThrow();
            ul.setIsDeleted(true);
            userLevelRepository.save(ul);
            ra.addFlashAttribute("successMessage", "Đã xóa cấp bậc khách hàng [" + ul.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa cấp bậc khách hàng: " + ex.getMessage());
        }
        return "redirect:/admin/user-levels";
    }

    /**
     * Alternative 6-10c: Khôi phục level (isDeleted = false)
     */
    @PostMapping("/user-levels/restore/{id}")
    public String restoreUserLevel(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            UserLevel ul = userLevelRepository.findById(id).orElseThrow();
            ul.setIsDeleted(false);
            userLevelRepository.save(ul);
            ra.addFlashAttribute("successMessage", "Khôi phục cấp bậc khách hàng [" + ul.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể khôi phục cấp bậc khách hàng: " + ex.getMessage());
        }
        return "redirect:/admin/user-levels";
    }

    @PostMapping("/user-levels/reset-defaults")
    public String resetDefaultUserLevels(RedirectAttributes ra) {
        record DefLevel(String name, int minPoint, BigDecimal discount) {}
        java.util.List<DefLevel> defaults = java.util.List.of(
            new DefLevel("Thành Viên Đồng", 0, BigDecimal.ZERO),
            new DefLevel("Thành Viên Bạc", 200, new BigDecimal("3")),
            new DefLevel("Thành Viên Vàng", 500, new BigDecimal("5")),
            new DefLevel("VIP Kim Cương", 1000, new BigDecimal("10"))
        );
        for (DefLevel d : defaults) {
            java.util.Optional<UserLevel> existing = userLevelRepository.findAll().stream()
                .filter(ul -> ul.getName().equalsIgnoreCase(d.name()))
                .findFirst();
            if (existing.isPresent()) {
                UserLevel ul = existing.get();
                ul.setMinPoint(d.minPoint());
                ul.setDiscount(d.discount());
                ul.setIsDeleted(false);
                userLevelRepository.save(ul);
            } else {
                userLevelRepository.save(UserLevel.builder().name(d.name()).minPoint(d.minPoint()).discount(d.discount()).isDeleted(false).build());
            }
        }
        ra.addFlashAttribute("successMessage", "Khôi phục dữ liệu cấp bậc khách hàng mẫu thành công!");
        return "redirect:/admin/user-levels";
    }

    // --- 6. Quản lý Hạng chi nhánh / Cấp độ cửa hàng (StoreLevel - UC_033) ---
    /**
     * Bước 1 - 5: Hiển thị bảng danh sách các store level, phân trang Next/Prev
     */
    @GetMapping({"/store-levels", "/store-level", "/cap-do-cua-hang", "/hang-chi-nhanh"})
    public String listStoreLevels(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<StoreLevel> allLevels = storeLevelRepository.findAllByOrderByMinPointAsc();

            java.util.List<StoreLevel> filtered = allLevels;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(sl ->
                        (sl.getName() != null && sl.getName().toLowerCase().contains(kw))
                        || String.valueOf(sl.getId()).contains(kw)
                        || (sl.getMinPoint() != null && String.valueOf(sl.getMinPoint()).contains(kw))
                ).toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<StoreLevel> pagedLevels = filtered.subList(fromIndex, toIndex);

            model.addAttribute("levels", pagedLevels);
            model.addAttribute("allLevels", allLevels);
            model.addAttribute("stores", storeRepository.findAll());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách cấp độ cửa hàng. Vui lòng thử lại!");
            model.addAttribute("levels", java.util.Collections.emptyList());
            model.addAttribute("allLevels", java.util.Collections.emptyList());
            model.addAttribute("stores", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/store-levels";
    }

    /**
     * Bước 6 & 7: Hiển thị form thêm level mới
     */
    @GetMapping("/store-levels/create")
    public String createStoreLevelForm(Model model) {
        model.addAttribute("storeLevel", StoreLevel.builder().minPoint(0).discount(BigDecimal.ZERO).build());
        model.addAttribute("isEdit", false);
        return "admin/store-level-form";
    }

    /**
     * Alternative 6a & 7a: Hiển thị form chỉnh sửa level
     */
    @GetMapping("/store-levels/edit/{id}")
    public String editStoreLevelForm(@PathVariable("id") Long id, Model model) {
        StoreLevel sl = storeLevelRepository.findById(id).orElse(null);
        if (sl == null) {
            return "redirect:/admin/store-levels";
        }
        model.addAttribute("storeLevel", sl);
        model.addAttribute("isEdit", true);
        return "admin/store-level-form";
    }

    /**
     * Bước 8 - 12: Thêm mới store level
     */
    @PostMapping("/store-levels/add")
    public String addStoreLevel(@RequestParam("name") String name,
                                @RequestParam("minPoint") Integer minPoint,
                                @RequestParam(value = "discount", required = false, defaultValue = "0") BigDecimal discount,
                                RedirectAttributes ra) {
        try {
            String cleanName = name != null ? name.trim() : "";
            if (cleanName.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng điền tên cấp độ cửa hàng!");
                return "redirect:/admin/store-levels";
            }
            int pointVal = minPoint != null ? minPoint : 0;
            StoreLevel sl = StoreLevel.builder().name(cleanName).minPoint(pointVal)
                    .discount(discount != null ? discount : BigDecimal.ZERO)
                    .isDeleted(false).build();
            storeLevelRepository.save(sl);
            ra.addFlashAttribute("successMessage", "Thêm mới cấp bậc cửa hàng [" + cleanName + "] thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi thêm cấp bậc cửa hàng: " + e.getMessage());
        }
        return "redirect:/admin/store-levels";
    }

    /**
     * Alternative 6a: Cập nhật store level
     */
    @RequestMapping(value = "/store-levels/update/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String updateStoreLevel(@PathVariable("id") Long id,
                                   @RequestParam("name") String name,
                                   @RequestParam("minPoint") Integer minPoint,
                                   @RequestParam(value = "discount", required = false, defaultValue = "0") BigDecimal discount,
                                   @RequestParam(value = "isDeleted", required = false) Boolean isDeleted,
                                   RedirectAttributes ra) {
        try {
            StoreLevel sl = storeLevelRepository.findById(id).orElse(null);
            if (sl == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy cấp bậc cửa hàng #" + id);
                return "redirect:/admin/store-levels";
            }
            String cleanName = name != null ? name.trim() : sl.getName();
            int pointVal = minPoint != null ? minPoint : (sl.getMinPoint() != null ? sl.getMinPoint() : 0);

            sl.setName(cleanName);
            sl.setMinPoint(pointVal);
            if (discount != null) {
                sl.setDiscount(discount);
            }
            if (isDeleted != null) {
                sl.setIsDeleted(isDeleted);
            }
            storeLevelRepository.save(sl);
            ra.addFlashAttribute("successMessage", "Cập nhật cấp bậc cửa hàng [" + cleanName + "] thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi cập nhật cấp bậc: " + e.getMessage());
        }
        return "redirect:/admin/store-levels";
    }

    /**
     * Alternative 6-10b: Xóa level (isDeleted = true)
     */
    @RequestMapping(value = "/store-levels/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteStoreLevel(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            StoreLevel sl = storeLevelRepository.findById(id).orElseThrow();
            sl.setIsDeleted(true);
            storeLevelRepository.save(sl);
            ra.addFlashAttribute("successMessage", "Đã xóa cấp bậc cửa hàng [" + sl.getName() + "] thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa cấp bậc: " + e.getMessage());
        }
        return "redirect:/admin/store-levels";
    }

    /**
     * Alternative 6-10c: Khôi phục level (isDeleted = false)
     */
    @PostMapping("/store-levels/restore/{id}")
    public String restoreStoreLevel(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            StoreLevel sl = storeLevelRepository.findById(id).orElseThrow();
            sl.setIsDeleted(false);
            storeLevelRepository.save(sl);
            ra.addFlashAttribute("successMessage", "Khôi phục cấp bậc cửa hàng [" + sl.getName() + "] thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi khôi phục cấp bậc: " + e.getMessage());
        }
        return "redirect:/admin/store-levels";
    }

    @PostMapping("/store-levels/reset-defaults")
    public String resetDefaultStoreLevels(RedirectAttributes ra) {
        try {
            record DefStoreLevel(String name, int minPoint, BigDecimal discount) {}
            java.util.List<DefStoreLevel> defaults = java.util.List.of(
                new DefStoreLevel("Tiêu chuẩn", 0, BigDecimal.ZERO),
                new DefStoreLevel("Cửa hàng uy tín", 10000000, new BigDecimal("5")),
                new DefStoreLevel("Cửa hàng xuất sắc", 30000000, new BigDecimal("10"))
            );
            java.util.List<StoreLevel> allLevels = storeLevelRepository.findAll();

            for (int i = 0; i < defaults.size(); i++) {
                DefStoreLevel d = defaults.get(i);
                StoreLevel target = null;
                for (StoreLevel sl : allLevels) {
                    String n = sl.getName().toLowerCase();
                    if (i == 0 && (n.contains("chuẩn") || (sl.getMinPoint() != null && sl.getMinPoint() == 0))) {
                        target = sl; break;
                    } else if (i == 1 && (n.contains("uy tín") || n.contains("bạc") || (sl.getMinPoint() != null && sl.getMinPoint() > 0 && sl.getMinPoint() < 20000000))) {
                        target = sl; break;
                    } else if (i == 2 && (n.contains("xuất sắc") || n.contains("vàng") || (sl.getMinPoint() != null && sl.getMinPoint() >= 20000000))) {
                        target = sl; break;
                    }
                }
                if (target != null) {
                    target.setName(d.name());
                    target.setMinPoint(d.minPoint());
                    target.setDiscount(d.discount());
                    target.setIsDeleted(false);
                    storeLevelRepository.save(target);
                } else {
                    storeLevelRepository.save(StoreLevel.builder()
                        .name(d.name())
                        .minPoint(d.minPoint())
                        .discount(d.discount())
                        .isDeleted(false)
                        .build());
                }
            }
            ra.addFlashAttribute("successMessage", "Khôi phục dữ liệu 3 hạng chuẩn: Tiêu chuẩn (0 ₫) · Cửa hàng uy tín (từ 10 triệu) · Cửa hàng xuất sắc (từ 30 triệu) thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khôi phục hạng chuẩn: " + e.getMessage());
        }
        return "redirect:/admin/store-levels";
    }

    // --- 7. Quản lý Thuộc tính sản phẩm (Style - UC_031) ---
    /**
     * Bước 1 - 4: Hiển thị danh sách style, phân trang Next/Prev
     */
    @GetMapping({"/styles", "/style", "/thuoc-tinh-san-pham"})
    public String listStyles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        try {
            java.util.List<Style> allStyles = styleRepository.findAll();

            java.util.List<Style> filtered = allStyles;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream().filter(s ->
                        (s.getName() != null && s.getName().toLowerCase().contains(kw))
                        || (s.getCategory() != null && s.getCategory().getName() != null && s.getCategory().getName().toLowerCase().contains(kw))
                        || String.valueOf(s.getId()).contains(kw)
                ).toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<Style> pagedStyles = filtered.subList(fromIndex, toIndex);

            model.addAttribute("styles", pagedStyles);
            model.addAttribute("allStyles", allStyles);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách thuộc tính sản phẩm. Vui lòng thử lại!");
            model.addAttribute("styles", java.util.Collections.emptyList());
            model.addAttribute("allStyles", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/styles";
    }

    /**
     * Bước 5 & 6: Chuyển đến form tạo style mới
     */
    @GetMapping("/styles/create")
    public String createStyleForm(Model model) {
        model.addAttribute("style", new Style());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", false);
        return "admin/style-form";
    }

    /**
     * Alternative 5a & 6a: Chuyển đến form chỉnh sửa style
     */
    @GetMapping("/styles/edit/{id}")
    public String editStyleForm(@PathVariable("id") Long id, Model model) {
        Style s = styleRepository.findById(id).orElse(null);
        if (s == null) {
            return "redirect:/admin/styles";
        }
        model.addAttribute("style", s);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", true);
        return "admin/style-form";
    }

    /**
     * Bước 7 - 11: Thêm mới style
     */
    @PostMapping("/styles/add")
    public String addStyle(@RequestParam("name") String name,
                           @RequestParam(value = "categoryId", required = false) Long categoryId,
                           RedirectAttributes ra) {
        try {
            Category cat = null;
            if (categoryId != null && categoryId > 0) {
                cat = categoryRepository.findById(categoryId).orElse(null);
            }
            Style s = Style.builder()
                    .name(name)
                    .category(cat)
                    .isDeleted(false)
                    .build();
            styleRepository.save(s);
            ra.addFlashAttribute("successMessage", "Thêm mới thuộc tính [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể thêm thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }

    /**
     * Alternative 6a: Cập nhật style
     */
    @PostMapping("/styles/update/{id}")
    public String updateStyle(@PathVariable("id") Long id,
                              @RequestParam("name") String name,
                              @RequestParam(value = "categoryId", required = false) Long categoryId,
                              RedirectAttributes ra) {
        try {
            Style s = styleRepository.findById(id).orElseThrow();
            s.setName(name);
            if (categoryId != null && categoryId > 0) {
                s.setCategory(categoryRepository.findById(categoryId).orElse(null));
            } else if (categoryId != null && categoryId == 0) {
                s.setCategory(null);
            }
            styleRepository.save(s);
            ra.addFlashAttribute("successMessage", "Cập nhật thuộc tính [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể cập nhật thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }

    /**
     * Alternative 5-9b: Xóa style (isDeleted = true)
     */
    @PostMapping("/styles/delete/{id}")
    public String deleteStyle(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Style s = styleRepository.findById(id).orElseThrow();
            s.setIsDeleted(true);
            styleRepository.save(s);
            ra.addFlashAttribute("successMessage", "Đã xóa thuộc tính [" + s.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }

    /**
     * Alternative 5-9c: Khôi phục style (isDeleted = false)
     */
    @PostMapping("/styles/restore/{id}")
    public String restoreStyle(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Style s = styleRepository.findById(id).orElseThrow();
            s.setIsDeleted(false);
            styleRepository.save(s);
            ra.addFlashAttribute("successMessage", "Khôi phục thuộc tính [" + s.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể khôi phục thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }

    /**
     * Alternative 5d & 6d1: Chọn nút Detail để xem danh sách các giá trị của thuộc tính (style value)
     */
    @GetMapping("/styles/{id}/values")
    public String listStyleValues(@PathVariable("id") Long id,
                                  @RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "size", defaultValue = "10") int size,
                                  Model model) {
        try {
            Style style = styleRepository.findById(id).orElse(null);
            if (style == null) {
                return "redirect:/admin/styles";
            }
            java.util.List<StyleValue> allValues = styleValueRepository.findByStyleId(id);
            java.util.List<StyleValue> filtered = allValues;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                filtered = filtered.stream()
                        .filter(v -> v.getName() != null && v.getName().toLowerCase().contains(kw))
                        .toList();
            }

            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            java.util.List<StyleValue> pagedValues = filtered.subList(fromIndex, toIndex);

            model.addAttribute("style", style);
            model.addAttribute("styleValues", pagedValues);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách giá trị thuộc tính. Vui lòng thử lại!");
            model.addAttribute("styleValues", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
        }
        return "admin/style-values";
    }

    /**
     * Alternative 6d2 & 6d3: Thêm mới style value
     */
    @PostMapping("/styles/{styleId}/values/add")
    public String addStyleValue(@PathVariable("styleId") Long styleId,
                                @RequestParam("name") String name,
                                RedirectAttributes ra) {
        try {
            Style style = styleRepository.findById(styleId).orElseThrow();
            StyleValue val = StyleValue.builder()
                    .name(name)
                    .style(style)
                    .isDeleted(false)
                    .build();
            styleValueRepository.save(val);
            ra.addFlashAttribute("successMessage", "Thêm mới giá trị thuộc tính [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể thêm giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Alternative 6e2 & 6e3: Sửa style value
     */
    @PostMapping("/styles/{styleId}/values/update/{valueId}")
    public String updateStyleValue(@PathVariable("styleId") Long styleId,
                                  @PathVariable("valueId") Long valueId,
                                  @RequestParam("name") String name,
                                  RedirectAttributes ra) {
        try {
            StyleValue val = styleValueRepository.findById(valueId).orElseThrow();
            val.setName(name);
            styleValueRepository.save(val);
            ra.addFlashAttribute("successMessage", "Cập nhật giá trị thuộc tính [" + name + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể cập nhật giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Alternative 6f2-9: Xóa style value (isDeleted = true)
     */
    @PostMapping("/styles/{styleId}/values/delete/{valueId}")
    public String deleteStyleValue(@PathVariable("styleId") Long styleId,
                                  @PathVariable("valueId") Long valueId,
                                  RedirectAttributes ra) {
        try {
            StyleValue val = styleValueRepository.findById(valueId).orElseThrow();
            val.setIsDeleted(true);
            styleValueRepository.save(val);
            ra.addFlashAttribute("successMessage", "Đã xóa giá trị thuộc tính [" + val.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Alternative 6g2-9: Khôi phục style value (isDeleted = false)
     */
    @PostMapping("/styles/{styleId}/values/restore/{valueId}")
    public String restoreStyleValue(@PathVariable("styleId") Long styleId,
                                   @PathVariable("valueId") Long valueId,
                                   RedirectAttributes ra) {
        try {
            StyleValue val = styleValueRepository.findById(valueId).orElseThrow();
            val.setIsDeleted(false);
            styleValueRepository.save(val);
            ra.addFlashAttribute("successMessage", "Khôi phục giá trị thuộc tính [" + val.getName() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể khôi phục giá trị thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles/" + styleId + "/values";
    }

    /**
     * Bổ sung thuộc tính mặc định: Đồng bộ an toàn 4 nhóm thuộc tính cốt lõi và các giá trị chuẩn
     * Tuyệt đối không xóa hay ghi đè dữ liệu người dùng đã tạo
     */
    @PostMapping({"/styles/seed-defaults", "/styles/reset-defaults"})
    public String seedDefaultStyles(RedirectAttributes ra) {
        try {
            // 1. Chuẩn hóa tên cũ sang tên mới thống nhất
            styleRepository.findAll().forEach(s -> {
                String n = s.getName().trim();
                if (n.equalsIgnoreCase("Tone màu hoa") || n.equalsIgnoreCase("Tone màu thiết kế")) {
                    s.setName("Màu sắc");
                    s.setIsDeleted(false);
                    styleRepository.save(s);
                } else if (n.equalsIgnoreCase("Kiểu dáng cắm") || n.equalsIgnoreCase("Kiểu dáng hoa")) {
                    s.setName("Kiểu dáng");
                    s.setIsDeleted(false);
                    styleRepository.save(s);
                } else if (n.equalsIgnoreCase("Kích cỡ bó hoa") || n.equalsIgnoreCase("Kích cỡ thiết kế")) {
                    s.setName("Kích cỡ");
                    s.setIsDeleted(false);
                    styleRepository.save(s);
                }
            });

            // 2. Định nghĩa 4 nhóm chính và các nhóm bổ sung cùng giá trị mẫu
            java.util.Map<String, java.util.List<String>> defaultGroups = new java.util.LinkedHashMap<>();
            defaultGroups.put("Loại hoa", java.util.List.of(
                    "Hướng dương", "Hoa hồng", "Tulip", "Cẩm tú cầu", "Lan hồ điệp", "Calla Lily", "Hoa Baby"
            ));
            defaultGroups.put("Kiểu dáng", java.util.List.of(
                    "Bó hoa", "Giỏ hoa", "Bình hoa", "Hộp hoa", "Chậu hoa", "Kệ hoa"
            ));
            defaultGroups.put("Màu sắc", java.util.List.of(
                    "Vàng", "Hồng", "Trắng", "Đỏ", "Tím", "Xanh", "Cam"
            ));
            defaultGroups.put("Dịp tặng", java.util.List.of(
                    "Sinh nhật", "Tốt nghiệp", "Khai trương", "Tình yêu", "Chúc mừng", "Đám cưới", "Chia buồn"
            ));
            defaultGroups.put("Phong cách thiết kế", java.util.List.of(
                    "Hàn Quốc hiện đại", "Vintage cổ điển", "Tối giản Minimalist", "Châu Âu sang trọng"
            ));
            defaultGroups.put("Kích cỡ", java.util.List.of(
                    "Size S (Nhỏ)", "Size M (Vừa)", "Size L (Lớn)"
            ));

            int addedGroups = 0;
            int addedValues = 0;

            for (java.util.Map.Entry<String, java.util.List<String>> entry : defaultGroups.entrySet()) {
                String groupName = entry.getKey();
                java.util.List<String> values = entry.getValue();

                java.util.Optional<Style> existingStyleOpt = styleRepository.findAll().stream()
                        .filter(s -> s.getName().equalsIgnoreCase(groupName))
                        .findFirst();

                Style style;
                if (existingStyleOpt.isPresent()) {
                    style = existingStyleOpt.get();
                    if (Boolean.TRUE.equals(style.getIsDeleted())) {
                        style.setIsDeleted(false);
                        styleRepository.save(style);
                    }
                } else {
                    style = styleRepository.save(Style.builder()
                            .name(groupName)
                            .isDeleted(false)
                            .build());
                    addedGroups++;
                }

                java.util.List<StyleValue> existingValues = styleValueRepository.findByStyleId(style.getId());
                java.util.Set<String> existingNames = existingValues.stream()
                        .map(v -> v.getName().toLowerCase().trim())
                        .collect(java.util.stream.Collectors.toSet());

                for (String valName : values) {
                    if (!existingNames.contains(valName.toLowerCase().trim())) {
                        styleValueRepository.save(StyleValue.builder()
                                .name(valName)
                                .style(style)
                                .isDeleted(false)
                                .build());
                        addedValues++;
                    }
                }
            }

            ra.addFlashAttribute("successMessage", "Đã bổ sung thuộc tính mặc định thành công! (" + addedGroups + " nhóm mới, " + addedValues + " giá trị mới). Toàn bộ dữ liệu bạn đã tạo vẫn được giữ nguyên an toàn.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể bổ sung thuộc tính: " + ex.getMessage());
        }
        return "redirect:/admin/styles";
    }
}
