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
public class AdminStoreLevelController {
    private final StoreLevelRepository storeLevelRepository;
    private final StoreRepository storeRepository;

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
    @PostMapping("/store-levels/update/{id}")
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
    @PostMapping("/store-levels/delete/{id}")
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

}

