package com.finalproject.controller.admin;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final com.finalproject.service.AccountProvisioningService provisioning;
    private final com.finalproject.service.CustomerAccountClosureService accountClosure;
    private final com.finalproject.service.AccountDeletionService accountDeletion;

    @GetMapping
    public String listUsers(@RequestParam(value = "tab", required = false, defaultValue = "users") String tab,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "role", required = false) String roleStr,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "20") int size,
                            Model model) {
        size = Math.max(1, Math.min(size, 50));
        final String search = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        final String normalizedRole = roleStr == null || roleStr.isBlank() ? "ALL" : roleStr.trim().toUpperCase(java.util.Locale.ROOT);
        final Role selectedRole = "ALL".equals(normalizedRole) ? null : Role.valueOf(normalizedRole);
        try {
            org.springframework.data.jpa.domain.Specification<User> filters = (root, query, cb) -> {
                var roleFilter = selectedRole == null ? cb.conjunction() : cb.equal(root.get("role"), selectedRole);
                if (search.isEmpty()) return roleFilter;
                String pattern = "%" + search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                return cb.and(roleFilter, cb.or(
                        cb.like(cb.lower(root.get("username")), pattern, '\\'),
                        cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\'),
                        cb.like(root.get("phone"), pattern, '\\')));
            };
            Page<User> userPage = com.finalproject.utils.StorePages.load(Math.max(0, page), size,
                    pageable -> userRepository.findAll(filters, pageable));

            model.addAttribute("activeTab", "users");
            model.addAttribute("users", userPage.getContent());
            model.addAttribute("userPage", userPage);
            model.addAttribute("currentPage", userPage.getNumber());
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("totalElements", userPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentRole", normalizedRole);
            model.addAttribute("roles", Role.values());
            model.addAttribute("dataLoadError", false);
        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách người dùng. Vui lòng thử lại!");
            model.addAttribute("users", Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
            model.addAttribute("roles", Role.values());
        }

        return "admin/users";
    }

    @PostMapping("/toggle-active/{id}")
    public String toggleActive(@PathVariable("id") Long id, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElseThrow();
        if (user.isRetiredDeliveryAccount()) {
            ra.addFlashAttribute("errorMessage", "Tài khoản giao hàng đã ngừng sử dụng và được giữ ở trạng thái vô hiệu hóa.");
            return "redirect:/admin/users?tab=users";
        }
        if (user.hasRole(Role.ADMIN) || "admin".equalsIgnoreCase(user.getUsername())) {
            ra.addFlashAttribute("errorMessage", "Không thể khóa tài khoản Quản trị viên tối cao!");
            return "redirect:/admin/users?tab=users";
        }
        if (Boolean.TRUE.equals(user.getIsActive())) {
            var result = accountClosure.disable(id, false);
            ra.addFlashAttribute("successMessage", "Đã khóa tài khoản [" + user.getUsername() + "]. " + result.message());
            return "redirect:/admin/users?tab=users";
        }
        user.setIsActive(true);
        user.setAccountDisabledByAdmin(false);
        userRepository.save(user);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tài khoản [" + user.getUsername() + "] thành [" + (user.getIsActive() ? "Hoạt động" : "Bị khóa") + "]");
        return "redirect:/admin/users?tab=users";
    }

    @PostMapping("/create")
    public String createAccount(Principal principal, @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String fullName,
            @RequestParam(defaultValue = "") String phone, @RequestParam(defaultValue = "") String password,
            @RequestParam(defaultValue = "") String role, RedirectAttributes ra) {
        try {
            provisioning.create(principal.getName(), username, email, fullName, phone, password, role);
            ra.addFlashAttribute("successMessage", "Đã tạo tài khoản mới. Vai trò của tài khoản được giữ cố định.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            ra.addFlashAttribute("errorMessage", "Tên đăng nhập hoặc email đã được sử dụng.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        try {
            String username = accountDeletion.delete(id, principal.getName());
            ra.addFlashAttribute("successMessage", "Đã xóa tài khoản [" + username + "] thành công.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa tài khoản. Vui lòng thử lại.");
        }
        return "redirect:/admin/users?tab=users";
    }
}
