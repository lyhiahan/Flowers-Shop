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
public class AdminUserLevelController {
    private final UserLevelRepository userLevelRepository;

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

}

