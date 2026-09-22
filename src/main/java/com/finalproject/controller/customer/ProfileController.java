package com.finalproject.controller.customer;

import com.finalproject.dto.request.ProfileUpdateRequest;
import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/customer/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public String viewProfile(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại"));
        model.addAttribute("user", user);
        return "customer/profile";
    }

    @PostMapping("/update")
    public String updateProfile(Principal principal,
                                @Valid @ModelAttribute("user") ProfileUpdateRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        if (bindingResult.hasErrors()) {
            return "customer/profile";
        }

        try {
            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
            user.setAddress(request.getAddress());

            // Nếu người dùng có upload ảnh đại diện mới
            if (request.getAvatarFile() != null && !request.getAvatarFile().isEmpty()) {
                String avatarUrl = cloudinaryService.uploadImage(request.getAvatarFile());
                user.setAvatarUrl(avatarUrl);
            }

            userRepository.save(user);
            ra.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi cập nhật ảnh/thông tin: " + ex.getMessage());
        }

        return "redirect:/customer/profile";
    }
}