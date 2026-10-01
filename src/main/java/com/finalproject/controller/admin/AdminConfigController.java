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

    // --- 1. Quản lý Loại sản phẩm / Danh mục (Categories) ---
    @GetMapping({"/categories", "/category", "/loai-san-pham"})
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/categories";
    }

    @PostMapping("/categories/add")
    public String addCategory(@RequestParam("name") String name,
                              @RequestParam(value = "description", required = false) String description,
                              RedirectAttributes ra) {
        Category cat = Category.builder().name(name).description(description).build();
        categoryRepository.save(cat);
        ra.addFlashAttribute("successMessage", "Thêm loại sản phẩm [" + name + "] thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable("id") Long id,
                                @RequestParam("name") String name,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes ra) {
        Category cat = categoryRepository.findById(id).orElseThrow();
        cat.setName(name);
        cat.setDescription(description);
        categoryRepository.save(cat);
        ra.addFlashAttribute("successMessage", "Cập nhật loại sản phẩm [" + name + "] thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Category cat = categoryRepository.findById(id).orElse(null);
            if (cat == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy loại sản phẩm cần xóa!");
                return "redirect:/admin/categories";
            }
            String catName = cat.getName();

            // Chuyển các sản phẩm liên kết sang loại hoa khác để bảo toàn dữ liệu và tránh lỗi khóa ngoại
            java.util.List<Product> products = productRepository.findByCategoryId(id);
            if (products != null && !products.isEmpty()) {
                Category fallback = categoryRepository.findAll().stream()
                        .filter(c -> !c.getId().equals(id))
                        .findFirst()
                        .orElseGet(() -> categoryRepository.save(Category.builder().name("Hoa Tươi Khác").description("Danh mục hoa tươi tổng hợp").build()));
                for (Product p : products) {
                    p.setCategory(fallback);
                    productRepository.save(p);
                }
            }

            categoryRepository.deleteById(id);
            ra.addFlashAttribute("successMessage", "Đã xóa loại sản phẩm [" + catName + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa loại sản phẩm này: " + ex.getMessage());
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
                categoryRepository.save(Category.builder().name(d.name()).description(d.desc()).build());
            }
        }
        ra.addFlashAttribute("successMessage", "Khôi phục toàn bộ danh sách 9 loại sản phẩm hoa tươi mẫu thành công!");
        return "redirect:/admin/categories";
    }

    // --- 2. Quản lý Chính sách hoa hồng (Đã gỡ bỏ - chuyển hướng sang Cấp độ chi nhánh) ---
    @GetMapping({"/commissions", "/commission", "/chinh-sach-hoa-hong"})
    public String listCommissions(Model model) {
        return "redirect:/admin/store-levels";
    }

    @PostMapping("/commissions/add")
    public String addCommission(@RequestParam("name") String name,
                                @RequestParam("cost") BigDecimal cost,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes ra) {
        Commission c = Commission.builder().name(name).cost(cost).description(description).isDeleted(false).build();
        commissionRepository.save(c);
        ra.addFlashAttribute("successMessage", "Thêm chính sách hoa hồng [" + name + "] thành công!");
        return "redirect:/admin/commissions";
    }

    @PostMapping("/commissions/update/{id}")
    public String updateCommission(@PathVariable("id") Long id,
                                   @RequestParam("name") String name,
                                   @RequestParam("cost") BigDecimal cost,
                                   @RequestParam(value = "description", required = false) String description,
                                   @RequestParam(value = "isDeleted", required = false) Boolean isDeleted,
                                   RedirectAttributes ra) {
        Commission c = commissionRepository.findById(id).orElseThrow();
        c.setName(name);
        c.setCost(cost);
        c.setDescription(description);
        if (isDeleted != null) {
            c.setIsDeleted(isDeleted);
        }
        commissionRepository.save(c);
        ra.addFlashAttribute("successMessage", "Cập nhật chính sách hoa hồng [" + name + "] thành công!");
        return "redirect:/admin/commissions";
    }

    @PostMapping("/commissions/delete/{id}")
    public String deleteCommission(@PathVariable("id") Long id, RedirectAttributes ra) {
        Commission c = commissionRepository.findById(id).orElseThrow();
        long storeCount = storeRepository.findAll().stream()
                .filter(s -> s.getCommission() != null && s.getCommission().getId().equals(id))
                .count();
        if (storeCount > 0) {
            c.setIsDeleted(true);
            commissionRepository.save(c);
            ra.addFlashAttribute("successMessage", "Chính sách hoa hồng [" + c.getName() + "] đang có " + storeCount + " cửa hàng áp dụng. Đã chuyển sang trạng thái 'Ngừng áp dụng' để bảo toàn lịch sử giao dịch.");
        } else {
            c.setIsDeleted(true);
            commissionRepository.save(c);
            ra.addFlashAttribute("successMessage", "Xóa chính sách hoa hồng [" + c.getName() + "] thành công!");
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

    // --- 3. Quản lý Đơn vị vận chuyển / Nhà vận chuyển (Deliveries) ---
    @GetMapping({"/deliveries", "/delivery", "/don-vi-van-chuyen"})
    public String listDeliveries(Model model) {
        model.addAttribute("deliveries", deliveryRepository.findByIsDeletedFalse());
        return "admin/deliveries";
    }

    @PostMapping("/deliveries/add")
    public String addDelivery(@RequestParam("name") String name,
                              @RequestParam("price") BigDecimal price,
                              @RequestParam(value = "description", required = false) String description,
                              RedirectAttributes ra) {
        Delivery d = Delivery.builder().name(name).price(price).description(description).isActive(true).isDeleted(false).build();
        deliveryRepository.save(d);
        ra.addFlashAttribute("successMessage", "Thêm đơn vị vận chuyển [" + name + "] thành công!");
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/update/{id}")
    public String updateDelivery(@PathVariable("id") Long id,
                                 @RequestParam("name") String name,
                                 @RequestParam("price") BigDecimal price,
                                 @RequestParam(value = "description", required = false) String description,
                                 RedirectAttributes ra) {
        Delivery d = deliveryRepository.findById(id).orElseThrow();
        d.setName(name);
        d.setPrice(price);
        d.setDescription(description);
        deliveryRepository.save(d);
        ra.addFlashAttribute("successMessage", "Cập nhật đơn vị vận chuyển [" + name + "] thành công! (Lưu ý: Mức phí mới chỉ áp dụng cho các đơn hàng tạo mới)");
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/toggle/{id}")
    public String toggleDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        Delivery d = deliveryRepository.findById(id).orElseThrow();
        boolean newActive = !Boolean.TRUE.equals(d.getIsActive());
        d.setIsActive(newActive);
        deliveryRepository.save(d);
        ra.addFlashAttribute("successMessage", "Đã " + (newActive ? "bật áp dụng" : "tạm dừng") + " đơn vị vận chuyển [" + d.getName() + "]. "
                + (newActive ? "Khách hàng đã có thể chọn phương thức này khi đặt hoa." : "Khách hàng sẽ không thể chọn phương thức này cho đơn mới; các đơn đã đặt trước đó vẫn giữ nguyên phí và thông tin giao hàng."));
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/delete/{id}")
    public String deleteDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        Delivery d = deliveryRepository.findById(id).orElseThrow();
        d.setIsDeleted(true);
        deliveryRepository.save(d);
        ra.addFlashAttribute("successMessage", "Xóa đơn vị vận chuyển [" + d.getName() + "] thành công!");
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

    // --- 5. Quản lý Cấp độ người dùng (UserLevel) ---
    @GetMapping("/user-levels")
    public String listUserLevels(Model model) {
        model.addAttribute("levels", userLevelRepository.findByIsDeletedFalseOrderByMinPointAsc());
        return "admin/user-levels";
    }

    @PostMapping("/user-levels/add")
    public String addUserLevel(@RequestParam("name") String name,
                               @RequestParam("minPoint") int minPoint,
                               @RequestParam("discount") BigDecimal discount,
                               RedirectAttributes ra) {
        UserLevel ul = UserLevel.builder().name(name).minPoint(minPoint).discount(discount).isDeleted(false).build();
        userLevelRepository.save(ul);
        ra.addFlashAttribute("successMessage", "Thêm cấp bậc khách hàng [" + name + "] thành công!");
        return "redirect:/admin/user-levels";
    }

    @PostMapping("/user-levels/update/{id}")
    public String updateUserLevel(@PathVariable("id") Long id,
                                  @RequestParam("name") String name,
                                  @RequestParam("minPoint") int minPoint,
                                  @RequestParam("discount") BigDecimal discount,
                                  RedirectAttributes ra) {
        UserLevel ul = userLevelRepository.findById(id).orElseThrow();
        ul.setName(name);
        ul.setMinPoint(minPoint);
        ul.setDiscount(discount);
        userLevelRepository.save(ul);
        ra.addFlashAttribute("successMessage", "Cập nhật cấp bậc khách hàng thành công!");
        return "redirect:/admin/user-levels";
    }

    @PostMapping("/user-levels/delete/{id}")
    public String deleteUserLevel(@PathVariable("id") Long id, RedirectAttributes ra) {
        UserLevel ul = userLevelRepository.findById(id).orElseThrow();
        ul.setIsDeleted(true);
        userLevelRepository.save(ul);
        ra.addFlashAttribute("successMessage", "Xóa cấp bậc khách hàng thành công!");
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

    // --- 6. Quản lý Hạng chi nhánh (StoreLevel) ---
    @GetMapping({"/store-levels", "/store-level", "/cap-do-cua-hang", "/hang-chi-nhanh"})
    public String listStoreLevels(Model model) {
        java.util.List<StoreLevel> sortedLevels = storeLevelRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(sl -> sl.getMinPoint() != null ? sl.getMinPoint() : 0))
                .toList();
        model.addAttribute("levels", sortedLevels);
        model.addAttribute("stores", storeRepository.findAll());
        return "admin/store-levels";
    }

    @PostMapping("/store-levels/add")
    public String addStoreLevel(@RequestParam("name") String name,
                                @RequestParam("minPoint") Integer minPoint,
                                @RequestParam(value = "discount", required = false, defaultValue = "0") BigDecimal discount,
                                RedirectAttributes ra) {
        try {
            String cleanName = name != null ? name.trim() : "";
            if (cleanName.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Tên hạng chi nhánh không được để trống!");
                return "redirect:/admin/store-levels";
            }
            boolean nameExists = storeLevelRepository.findAll().stream()
                    .anyMatch(sl -> sl.getName().equalsIgnoreCase(cleanName));
            if (nameExists) {
                ra.addFlashAttribute("errorMessage", "Tên hạng [" + cleanName + "] đã tồn tại trong hệ thống!");
                return "redirect:/admin/store-levels";
            }
            int pointVal = minPoint != null ? minPoint : 0;
            boolean pointExists = storeLevelRepository.findAll().stream()
                    .anyMatch(sl -> sl.getMinPoint() != null && sl.getMinPoint().equals(pointVal));
            if (pointExists) {
                ra.addFlashAttribute("errorMessage", "Điều kiện doanh số / điểm [" + pointVal + "] đã trùng với một hạng khác!");
                return "redirect:/admin/store-levels";
            }
            StoreLevel sl = StoreLevel.builder().name(cleanName).minPoint(pointVal)
                    .discount(discount != null ? discount : BigDecimal.ZERO)
                    .isDeleted(false).build();
            storeLevelRepository.save(sl);
            ra.addFlashAttribute("successMessage", "Thêm hạng chi nhánh [" + cleanName + "] thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi thêm hạng chi nhánh: " + e.getMessage());
        }
        return "redirect:/admin/store-levels";
    }

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
                ra.addFlashAttribute("errorMessage", "Không tìm thấy hạng chi nhánh #" + id);
                return "redirect:/admin/store-levels";
            }
            String cleanName = name != null ? name.trim() : sl.getName();
            if (cleanName.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Tên hạng không được để trống!");
                return "redirect:/admin/store-levels";
            }

            // Kiểm tra trùng tên với hạng khác
            boolean nameExists = storeLevelRepository.findAll().stream()
                    .anyMatch(other -> !other.getId().equals(id) && other.getName().equalsIgnoreCase(cleanName));
            if (nameExists) {
                ra.addFlashAttribute("errorMessage", "Tên hạng [" + cleanName + "] đã tồn tại ở một hạng khác!");
                return "redirect:/admin/store-levels";
            }

            // Kiểm tra trùng minPoint với hạng khác
            int pointVal = minPoint != null ? minPoint : (sl.getMinPoint() != null ? sl.getMinPoint() : 0);
            boolean pointExists = storeLevelRepository.findAll().stream()
                    .anyMatch(other -> !other.getId().equals(id) && other.getMinPoint() != null && other.getMinPoint().equals(pointVal));
            if (pointExists) {
                ra.addFlashAttribute("errorMessage", "Điều kiện doanh số / điểm [" + pointVal + "] đã trùng với một hạng khác!");
                return "redirect:/admin/store-levels";
            }

            sl.setName(cleanName);
            sl.setMinPoint(pointVal);
            if (discount != null) {
                sl.setDiscount(discount);
            }
            if (isDeleted != null) {
                if (sl.getMinPoint() != null && sl.getMinPoint() == 0) {
                    sl.setIsDeleted(false);
                } else {
                    sl.setIsDeleted(isDeleted);
                }
            }
            storeLevelRepository.save(sl);
            ra.addFlashAttribute("successMessage", "Cập nhật hạng chi nhánh [" + cleanName + "] thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi cập nhật hạng: " + e.getMessage());
        }
        return "redirect:/admin/store-levels";
    }

    @RequestMapping(value = "/store-levels/toggle/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String toggleStoreLevel(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            StoreLevel sl = storeLevelRepository.findById(id).orElseThrow();
            if (sl.getMinPoint() != null && sl.getMinPoint() == 0) {
                ra.addFlashAttribute("errorMessage", "Hạng [" + sl.getName() + "] là hạng mặc định cơ sở của hệ thống, không thể ngừng áp dụng!");
                return "redirect:/admin/store-levels";
            }
            boolean newDeleted = !Boolean.TRUE.equals(sl.getIsDeleted());
            sl.setIsDeleted(newDeleted);
            storeLevelRepository.save(sl);
            ra.addFlashAttribute("successMessage", "Đã chuyển trạng thái hạng [" + sl.getName() + "] sang [" + (newDeleted ? "Ngừng áp dụng" : "Đang áp dụng") + "]!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi chuyển trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/store-levels";
    }

    @RequestMapping(value = "/store-levels/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteStoreLevel(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            StoreLevel sl = storeLevelRepository.findById(id).orElseThrow();
            if (sl.getMinPoint() != null && sl.getMinPoint() == 0) {
                ra.addFlashAttribute("errorMessage", "Hạng [" + sl.getName() + "] là hạng mặc định cơ sở của hệ thống, không thể xóa!");
                return "redirect:/admin/store-levels";
            }
            long storeCount = storeRepository.findAll().stream()
                    .filter(s -> s.getStoreLevel() != null && s.getStoreLevel().getId().equals(id))
                    .count();
            sl.setIsDeleted(true);
            storeLevelRepository.save(sl);
            if (storeCount > 0) {
                ra.addFlashAttribute("successMessage", "Hạng [" + sl.getName() + "] đang có " + storeCount + " chi nhánh áp dụng. Hệ thống đã chuyển sang trạng thái [Ngừng áp dụng] để bảo toàn lịch sử.");
            } else {
                ra.addFlashAttribute("successMessage", "Đã chuyển hạng chi nhánh [" + sl.getName() + "] sang trạng thái Ngừng áp dụng!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa hạng: " + e.getMessage());
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

    // --- 7. Quản lý Thuộc tính sản phẩm (Style) ---
    @GetMapping("/styles")
    public String listStyles(Model model) {
        model.addAttribute("styles", styleRepository.findByIsDeletedFalse());
        return "admin/styles";
    }

    @PostMapping("/styles/add")
    public String addStyle(@RequestParam("name") String name, RedirectAttributes ra) {
        Style s = Style.builder().name(name).isDeleted(false).build();
        styleRepository.save(s);
        ra.addFlashAttribute("successMessage", "Thêm nhóm thuộc tính [" + name + "] thành công!");
        return "redirect:/admin/styles";
    }

    @PostMapping("/styles/update/{id}")
    public String updateStyle(@PathVariable("id") Long id,
                              @RequestParam("name") String name,
                              RedirectAttributes ra) {
        Style s = styleRepository.findById(id).orElseThrow();
        s.setName(name);
        styleRepository.save(s);
        ra.addFlashAttribute("successMessage", "Cập nhật nhóm thuộc tính thành công!");
        return "redirect:/admin/styles";
    }

    @PostMapping("/styles/delete/{id}")
    public String deleteStyle(@PathVariable("id") Long id, RedirectAttributes ra) {
        Style s = styleRepository.findById(id).orElseThrow();
        s.setIsDeleted(true);
        styleRepository.save(s);
        ra.addFlashAttribute("successMessage", "Xóa nhóm thuộc tính thành công!");
        return "redirect:/admin/styles";
    }

    @PostMapping("/styles/reset-defaults")
    public String resetDefaultStyles(RedirectAttributes ra) {
        java.util.List<String> defaultStyles = java.util.List.of(
            "Kiểu Dáng Cắm (Bó hoa, Giỏ hoa, Hộp hoa, Bình hoa, Kệ hoa, Chậu hoa)",
            "Tone Màu Thiết Kế (Pastel ngọt ngào, Tone Đỏ nồng cháy, Tone Vàng tài lộc, Tone Trắng tinh khôi)",
            "Phong Cách Thiết Kế (Hàn Quốc hiện đại, Vintage cổ điển, Châu Âu quý phái, Tối giản Minimalist)",
            "Kích Cỡ Thiết Kế (Size S Nhỏ xinh, Size M Tiêu chuẩn, Size L Sang trọng, Size VIP Khổng lồ)",
            "Loại Hoa Chủ Đạo (Hoa Hồng, Hoa Hướng Dương, Lan Hồ Điệp, Cẩm Tú Cầu, Hoa Tulip, Hoa Baby)"
        );
        for (String name : defaultStyles) {
            java.util.Optional<Style> existing = styleRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst();
            if (existing.isPresent()) {
                Style s = existing.get();
                s.setIsDeleted(false);
                styleRepository.save(s);
            } else {
                styleRepository.save(Style.builder().name(name).isDeleted(false).build());
            }
        }
        ra.addFlashAttribute("successMessage", "Khôi phục toàn bộ danh sách thuộc tính sản phẩm mẫu thành công!");
        return "redirect:/admin/styles";
    }
}
