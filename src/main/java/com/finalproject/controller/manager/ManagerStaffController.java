package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping({"/manager/staff", "/vendor/staff"})
@RequiredArgsConstructor
public class ManagerStaffController {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String listStaff(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = user.getStore() != null ? user.getStore() : storeRepository.findById(1L).orElse(null);

        List<User> staffList = store != null ? userRepository.findByStoreId(store.getId()) : List.of();

        model.addAttribute("store", store);
        model.addAttribute("staffList", staffList);
        return "manager/staff";
    }

    @PostMapping("/add")
    public String addStaff(Principal principal,
                           @RequestParam("username") String username,
                           @RequestParam(value = "fullName", required = false) String fullName,
                           @RequestParam(value = "email", required = false) String email,
                           @RequestParam(value = "phone", required = false) String phone,
                           @RequestParam(value = "password", defaultValue = "123456") String password,
                           @RequestParam(value = "role", defaultValue = "STAFF") String roleStr,
                           RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = user.getStore() != null ? user.getStore() : storeRepository.findById(1L).orElseThrow();

        // Kiểm tra xem username đã tồn tại chưa
        var existingOpt = userRepository.findByUsername(username.trim());
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            existing.setStore(store);
            userRepository.save(existing);
            ra.addFlashAttribute("successMessage", "Đã thêm nhân viên [" + existing.getFullName() + "] vào chi nhánh thành công!");
        } else {
            Role role = "SHIPPER".equalsIgnoreCase(roleStr) ? Role.SHIPPER : Role.MANAGER;

            User newStaff = User.builder()
                    .username(username.trim())
                    .fullName(fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : username.trim())
                    .email(email != null && !email.trim().isEmpty() ? email.trim() : username.trim() + "@bloomflower.vn")
                    .phone(phone)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .store(store)
                    .isEmailActive(true)
                    .build();

            userRepository.save(newStaff);
            ra.addFlashAttribute("successMessage", "Tạo tài khoản nhân viên mới [" + username + "] cho chi nhánh thành công!");
        }

        return "redirect:/manager/staff";
    }

    @PostMapping("/remove/{id}")
    public String removeStaff(@PathVariable("id") Long id, RedirectAttributes ra) {
        User staff = userRepository.findById(id).orElseThrow();
        staff.setStore(null);
        userRepository.save(staff);
        ra.addFlashAttribute("successMessage", "Đã xóa nhân viên khỏi chi nhánh thành công!");
        return "redirect:/manager/staff";
    }
}
