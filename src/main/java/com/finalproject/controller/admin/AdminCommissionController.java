package com.finalproject.controller.admin;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminCommissionController {
    private final CommissionRepository commissionRepository;
    private final StoreRepository storeRepository;

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
    @PostMapping("/commissions/update/{id}")
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
    @PostMapping("/commissions/delete/{id}")
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
            new DefComm("Gói cơ bản (Standard)", new BigDecimal("10"), "Tỷ lệ chiết khấu sàn 10% áp dụng cho chi nhánh mới gia nhập"),
            new DefComm("Gói đối tác thân thiết", new BigDecimal("8"), "Tỷ lệ chiết khấu ưu đãi 8% cho cửa hàng có trên 100 đơn/tháng"),
            new DefComm("Gói đối tác chiến lược", new BigDecimal("5"), "Tỷ lệ chiết khấu đặc biệt 5% cho chuỗi phân phối hoa tươi lớn"),
            new DefComm("Phí dịch vụ cố định theo đơn", new BigDecimal("25000"), "Mức thu cố định 25.000 ₫/đơn cho đơn hoa sự kiện")
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

}

