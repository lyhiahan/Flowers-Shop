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
    private final CommissionRepository commissionRepository;
    private final DeliveryRepository deliveryRepository;
    private final VoucherRepository voucherRepository;
    private final UserLevelRepository userLevelRepository;
    private final StoreLevelRepository storeLevelRepository;
    private final StyleRepository styleRepository;
    private final StyleValueRepository styleValueRepository;

    // --- 1. Quản lý Danh mục (Categories) ---
    @GetMapping("/categories")
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
        ra.addFlashAttribute("successMessage", "Thêm danh mục [" + name + "] thành công!");
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
        ra.addFlashAttribute("successMessage", "Cập nhật danh mục [" + name + "] thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            categoryRepository.deleteById(id);
            ra.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa danh mục đang có sản phẩm liên kết!");
        }
        return "redirect:/admin/categories";
    }

    // --- 2. Quản lý Chiết khấu App cho các shop (Commissions) ---
    @GetMapping("/commissions")
    public String listCommissions(Model model) {
        model.addAttribute("commissions", commissionRepository.findByIsDeletedFalse());
        return "admin/commissions";
    }

    @PostMapping("/commissions/add")
    public String addCommission(@RequestParam("name") String name,
                                @RequestParam("cost") BigDecimal cost,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes ra) {
        Commission c = Commission.builder().name(name).cost(cost).description(description).isDeleted(false).build();
        commissionRepository.save(c);
        ra.addFlashAttribute("successMessage", "Thêm chính sách chiết khấu [" + name + "] thành công!");
        return "redirect:/admin/commissions";
    }

    @PostMapping("/commissions/update/{id}")
    public String updateCommission(@PathVariable("id") Long id,
                                   @RequestParam("name") String name,
                                   @RequestParam("cost") BigDecimal cost,
                                   @RequestParam(value = "description", required = false) String description,
                                   RedirectAttributes ra) {
        Commission c = commissionRepository.findById(id).orElseThrow();
        c.setName(name);
        c.setCost(cost);
        c.setDescription(description);
        commissionRepository.save(c);
        ra.addFlashAttribute("successMessage", "Cập nhật chính sách chiết khấu thành công!");
        return "redirect:/admin/commissions";
    }

    @PostMapping("/commissions/delete/{id}")
    public String deleteCommission(@PathVariable("id") Long id, RedirectAttributes ra) {
        Commission c = commissionRepository.findById(id).orElseThrow();
        c.setIsDeleted(true);
        commissionRepository.save(c);
        ra.addFlashAttribute("successMessage", "Xóa chính sách chiết khấu thành công!");
        return "redirect:/admin/commissions";
    }

    // --- 3. Quản lý Nhà vận chuyển (Deliveries) ---
    @GetMapping("/deliveries")
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
        ra.addFlashAttribute("successMessage", "Thêm phương thức giao hàng [" + name + "] thành công!");
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
        ra.addFlashAttribute("successMessage", "Cập nhật phương thức giao hàng [" + name + "] thành công! (Lưu ý: Mức phí mới chỉ áp dụng cho các đơn hàng tạo mới)");
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/toggle/{id}")
    public String toggleDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        Delivery d = deliveryRepository.findById(id).orElseThrow();
        boolean newActive = !Boolean.TRUE.equals(d.getIsActive());
        d.setIsActive(newActive);
        deliveryRepository.save(d);
        ra.addFlashAttribute("successMessage", "Đã " + (newActive ? "bật áp dụng" : "tạm dừng") + " phương thức giao hàng [" + d.getName() + "]. "
                + (newActive ? "Khách hàng đã có thể chọn phương thức này khi đặt hoa." : "Khách hàng sẽ không thể chọn phương thức này cho đơn mới; các đơn đã đặt trước đó vẫn giữ nguyên phí và thông tin giao hàng."));
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/deliveries/delete/{id}")
    public String deleteDelivery(@PathVariable("id") Long id, RedirectAttributes ra) {
        Delivery d = deliveryRepository.findById(id).orElseThrow();
        d.setIsDeleted(true);
        deliveryRepository.save(d);
        ra.addFlashAttribute("successMessage", "Xóa phương thức giao hàng thành công!");
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
                                     @RequestParam(value = "quantity", defaultValue = "100") int quantity,
                                     @RequestParam("startDate") String startDate,
                                     @RequestParam("endDate") String endDate,
                                     RedirectAttributes ra) {
        Voucher v = Voucher.builder()
                .code(code.trim().toUpperCase())
                .name(name.trim())
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderValue(minOrderValue)
                .store(null) // Voucher toàn sàn
                .quantity(quantity)
                .usedCount(0)
                .startDate(LocalDate.parse(startDate))
                .endDate(LocalDate.parse(endDate))
                .isActive(true)
                .build();
        voucherRepository.save(v);
        ra.addFlashAttribute("successMessage", "Thêm mã khuyến mãi toàn sàn [" + code + "] thành công!");
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

    // --- 6. Quản lý Cấp độ cửa hàng (StoreLevel) ---
    @GetMapping("/store-levels")
    public String listStoreLevels(Model model) {
        model.addAttribute("levels", storeLevelRepository.findByIsDeletedFalseOrderByMinPointAsc());
        return "admin/store-levels";
    }

    @PostMapping("/store-levels/add")
    public String addStoreLevel(@RequestParam("name") String name,
                                @RequestParam("minPoint") int minPoint,
                                @RequestParam("discount") BigDecimal discount,
                                RedirectAttributes ra) {
        StoreLevel sl = StoreLevel.builder().name(name).minPoint(minPoint).discount(discount).isDeleted(false).build();
        storeLevelRepository.save(sl);
        ra.addFlashAttribute("successMessage", "Thêm cấp bậc chi nhánh [" + name + "] thành công!");
        return "redirect:/admin/store-levels";
    }

    @PostMapping("/store-levels/update/{id}")
    public String updateStoreLevel(@PathVariable("id") Long id,
                                   @RequestParam("name") String name,
                                   @RequestParam("minPoint") int minPoint,
                                   @RequestParam("discount") BigDecimal discount,
                                   RedirectAttributes ra) {
        StoreLevel sl = storeLevelRepository.findById(id).orElseThrow();
        sl.setName(name);
        sl.setMinPoint(minPoint);
        sl.setDiscount(discount);
        storeLevelRepository.save(sl);
        ra.addFlashAttribute("successMessage", "Cập nhật cấp bậc chi nhánh thành công!");
        return "redirect:/admin/store-levels";
    }

    @PostMapping("/store-levels/delete/{id}")
    public String deleteStoreLevel(@PathVariable("id") Long id, RedirectAttributes ra) {
        StoreLevel sl = storeLevelRepository.findById(id).orElseThrow();
        sl.setIsDeleted(true);
        storeLevelRepository.save(sl);
        ra.addFlashAttribute("successMessage", "Xóa cấp bậc chi nhánh thành công!");
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
}
