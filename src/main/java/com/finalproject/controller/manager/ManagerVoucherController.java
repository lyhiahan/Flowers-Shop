package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.Voucher;
import com.finalproject.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping({"/manager/vouchers", "/vendor/vouchers"})
@RequiredArgsConstructor
public class ManagerVoucherController {

    private final VoucherService voucherService;
    private final com.finalproject.repository.VoucherRepository voucherRepository;
    private final com.finalproject.service.StoreAccessService storeAccess;

    @GetMapping
    public String listVouchers(Principal principal, Model model) {
        Long storeId = storeAccess.assignedStore().getId();

        List<Voucher> vouchers = voucherService.getVouchersByStore(storeId);
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("store", storeAccess.assignedStore());
        return "manager/vouchers";
    }

    @PostMapping("/add")
    public String createVoucher(Principal principal,
                                @RequestParam("code") String code,
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
            Store store = storeAccess.assignedStore();

            String cleanCode = code != null ? code.trim().toUpperCase() : "";
            if (cleanCode.length() < 3 || cleanCode.length() > 30 || !cleanCode.matches("^[A-Z0-9_-]+$")) {
                ra.addFlashAttribute("errorMessage", "Mã khuyến mãi không hợp lệ! Vui lòng nhập từ 3 đến 30 ký tự (chữ cái, chữ số, gạch '-' hoặc '_', không có khoảng trắng).");
                return "redirect:/manager/vouchers";
            }
            if (voucherRepository.existsByCode(cleanCode)) {
                ra.addFlashAttribute("errorMessage", "Mã khuyến mãi [" + cleanCode + "] đã tồn tại! Vui lòng chọn mã khác.");
                return "redirect:/manager/vouchers";
            }

            String cleanName = name != null ? name.trim() : "";
            if (cleanName.length() < 3 || cleanName.length() > 150) {
                ra.addFlashAttribute("errorMessage", "Tên chương trình khuyến mãi phải từ 3 đến 150 ký tự!");
                return "redirect:/manager/vouchers";
            }

            if ("PERCENT".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                    ra.addFlashAttribute("errorMessage", "Tỷ lệ giảm giá % phải lớn hơn 0 và không quá 100%!");
                    return "redirect:/manager/vouchers";
                }
            } else if ("FIXED_AMOUNT".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.valueOf(1000)) < 0) {
                    ra.addFlashAttribute("errorMessage", "Số tiền giảm cố định phải từ 1.000 ₫ trở lên!");
                    return "redirect:/manager/vouchers";
                }
                if (minOrderValue != null && minOrderValue.compareTo(BigDecimal.ZERO) > 0 && discountValue.compareTo(minOrderValue) >= 0) {
                    ra.addFlashAttribute("errorMessage", "Mức giảm cố định không được lớn hơn hoặc bằng giá trị đơn hàng tối thiểu!");
                    return "redirect:/manager/vouchers";
                }
            } else if ("FREE_SHIP".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                    discountValue = BigDecimal.valueOf(30000);
                }
            }

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (start.isAfter(end)) {
                ra.addFlashAttribute("errorMessage", "Ngày kết thúc phải sau hoặc cùng ngày với ngày bắt đầu!");
                return "redirect:/manager/vouchers";
            }

            Voucher voucher = Voucher.builder()
                    .code(cleanCode)
                    .name(cleanName)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .minOrderValue(minOrderValue)
                    .maxDiscountAmount(maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0 ? maxDiscountAmount : null)
                    .store(store)
                    .quantity(quantity)
                    .usedCount(0)
                    .startDate(start)
                    .endDate(end)
                    .isActive(true)
                    .build();

            voucherService.createVoucher(voucher);
            ra.addFlashAttribute("successMessage", "Tạo chương trình khuyến mãi [" + cleanCode + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi tạo mã khuyến mãi: " + ex.getMessage());
        }
        return "redirect:/manager/vouchers";
    }

    @PostMapping("/update/{id}")
    public String updateVoucher(@PathVariable("id") Long id,
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
            Voucher v = voucherRepository.findById(id).orElseThrow();
            storeAccess.requireManager(v.getStore());

            String cleanName = name != null ? name.trim() : "";
            if (cleanName.length() < 3 || cleanName.length() > 150) {
                ra.addFlashAttribute("errorMessage", "Tên chương trình khuyến mãi phải từ 3 đến 150 ký tự!");
                return "redirect:/manager/vouchers";
            }

            if ("PERCENT".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                    ra.addFlashAttribute("errorMessage", "Tỷ lệ giảm giá % phải lớn hơn 0 và không quá 100%!");
                    return "redirect:/manager/vouchers";
                }
            } else if ("FIXED_AMOUNT".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.valueOf(1000)) < 0) {
                    ra.addFlashAttribute("errorMessage", "Số tiền giảm cố định phải từ 1.000 ₫ trở lên!");
                    return "redirect:/manager/vouchers";
                }
                if (minOrderValue != null && minOrderValue.compareTo(BigDecimal.ZERO) > 0 && discountValue.compareTo(minOrderValue) >= 0) {
                    ra.addFlashAttribute("errorMessage", "Mức giảm cố định không được lớn hơn hoặc bằng giá trị đơn hàng tối thiểu!");
                    return "redirect:/manager/vouchers";
                }
            } else if ("FREE_SHIP".equals(discountType)) {
                if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                    discountValue = BigDecimal.valueOf(30000);
                }
            }

            if (quantity < v.getUsedCount()) {
                ra.addFlashAttribute("errorMessage", "Số lượng phát hành (" + quantity + ") không được nhỏ hơn số lượt đã dùng (" + v.getUsedCount() + ")!");
                return "redirect:/manager/vouchers";
            }

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (start.isAfter(end)) {
                ra.addFlashAttribute("errorMessage", "Ngày kết thúc phải sau hoặc cùng ngày với ngày bắt đầu!");
                return "redirect:/manager/vouchers";
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
            ra.addFlashAttribute("successMessage", "Cập nhật mã khuyến mãi [" + v.getCode() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + ex.getMessage());
        }
        return "redirect:/manager/vouchers";
    }

    @PostMapping("/toggle/{id}")
    public String toggleVoucher(@PathVariable("id") Long id, RedirectAttributes ra) {
        Voucher v = voucherRepository.findById(id).orElseThrow();
        storeAccess.requireManager(v.getStore());
        boolean newStatus = !Boolean.TRUE.equals(v.getIsActive());
        v.setIsActive(newStatus);
        voucherRepository.save(v);
        ra.addFlashAttribute("successMessage", "Đã " + (newStatus ? "bật" : "tắt") + " mã khuyến mãi [" + v.getCode() + "]!");
        return "redirect:/manager/vouchers";
    }

    @PostMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Long id, RedirectAttributes ra) {
        Voucher v = voucherRepository.findById(id).orElseThrow();
        storeAccess.requireManager(v.getStore());
        String code = v.getCode();
        if (v.getUsedCount() != null && v.getUsedCount() > 0) {
            v.setIsActive(false);
            voucherRepository.save(v);
            ra.addFlashAttribute("successMessage", "Mã [" + code + "] đã có " + v.getUsedCount() + " lượt dùng nên đã được chuyển sang [Đã tắt] thay vì xóa.");
        } else {
            voucherRepository.delete(v);
            ra.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn mã khuyến mãi [" + code + "]!");
        }
        return "redirect:/manager/vouchers";
    }
}
