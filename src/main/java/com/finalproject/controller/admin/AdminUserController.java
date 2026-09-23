package com.finalproject.controller.admin;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "role", required = false) String roleStr,
                            Model model) {
        List<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword.trim(), keyword.trim(), keyword.trim());
        } else if (roleStr != null && !roleStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(roleStr)) {
            try {
                Role role = Role.valueOf(roleStr.toUpperCase());
                users = userRepository.findByRole(role);
            } catch (Exception ex) {
                users = userRepository.findAll();
            }
        } else {
            users = userRepository.findAll();
        }

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentRole", roleStr != null ? roleStr : "ALL");
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/toggle-active/{id}")
    public String toggleActive(@PathVariable("id") Long id, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElseThrow();
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            ra.addFlashAttribute("errorMessage", "Không thể khóa tài khoản Quản trị viên tối cao!");
            return "redirect:/admin/users";
        }
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepository.save(user);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tài khoản [" + user.getUsername() + "] thành [" + (user.getIsActive() ? "Hoạt động" : "Bị khóa") + "]");
        return "redirect:/admin/users";
    }
}
