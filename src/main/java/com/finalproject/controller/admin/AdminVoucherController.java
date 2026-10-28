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
public class AdminVoucherController {
    private final VoucherRepository voucherRepository;
    private final com.finalproject.service.VoucherService voucherService;

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

            final BigDecimal finalDiscountValue = discountValue;
            Voucher updated = voucherService.updateVoucherDetails(id, existing -> {
                if (quantity < existing.getUsedCount()) {
                    throw new IllegalArgumentException("Số lượng phát hành (" + quantity + ") không được nhỏ hơn số lượt mã đã sử dụng (" + existing.getUsedCount() + " lượt)!");
                }
                existing.setName(cleanName);
                existing.setDiscountType(discountType);
                existing.setDiscountValue(finalDiscountValue);
                existing.setMinOrderValue(minOrderValue);
                existing.setMaxDiscountAmount(maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0 ? maxDiscountAmount : null);
                existing.setQuantity(quantity);
                existing.setStartDate(start);
                existing.setEndDate(end);
                existing.setIsActive(isActive);
            });

            ra.addFlashAttribute("successMessage", "Đã cập nhật mã khuyến mãi [" + updated.getCode() + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi cập nhật mã khuyến mãi: " + ex.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/toggle/{id}")
    public String togglePlatformVoucher(@PathVariable("id") Long id, RedirectAttributes ra) {
        Voucher v = voucherRepository.findById(id).orElse(null);
        if (v == null) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy mã khuyến mãi!");
            return "redirect:/admin/vouchers";
        }
        boolean newStatus = voucherService.toggleVoucherActive(id);
        ra.addFlashAttribute("successMessage", "Đã " + (newStatus ? "bật kích hoạt" : "tắt") + " mã khuyến mãi [" + v.getCode() + "] thành công!");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/delete/{id}")
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

}

