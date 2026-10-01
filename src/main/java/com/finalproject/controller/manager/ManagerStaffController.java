package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller triển khai Use Case UC_020: Quản lý nhân viên của cửa hàng (Store Management – Staff)
 */
@Controller
@RequestMapping({"/manager/staff", "/vendor/staff", "/manager/staffs", "/vendor/staffs"})
@RequiredArgsConstructor
@Slf4j
public class ManagerStaffController {

    private final UserRepository userRepository;
    private final StoreAccessService storeAccess;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Bước 6 & 7: Vào trang quản lý nhân viên cửa hàng
     * Bước 8, 9: Xem thông tin chủ cửa hàng (Shop owner)
     * Bước 10, 11: Xem danh sách nhân viên cửa hàng (Shop staffs)
     * Bước 12: Phân trang danh sách nhân viên (Next, Previous, số trang)
     */
    @GetMapping
    public String listStaff(@RequestParam(value = "tab", defaultValue = "staffs") String tab,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "6") int size,
                            Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) {
            return "redirect:/manager/stores";
        }
        storeAccess.requireStore(store);

        // Kiểm tra xem người quản lý có phải là Chủ cửa hàng hay Đối tác (Partner)
        boolean isOwner = (store.getOwner() != null && user.getId().equals(store.getOwner().getId()))
                || user.getRole() == Role.ADMIN;
        boolean isPartner = !isOwner;

        // Chủ cửa hàng (Shop owner)
        User shopOwner = store.getOwner();

        // Lấy danh sách nhân viên của chi nhánh (loại trừ chủ sở hữu)
        List<User> allStaff = userRepository.findByStoreId(store.getId());
        List<User> staffOnlyList = allStaff.stream()
                .filter(s -> shopOwner == null || !s.getId().equals(shopOwner.getId()))
                .toList();

        // Phân trang danh sách nhân viên (Bước 12)
        int totalElements = staffOnlyList.size();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        if (page < 0) page = 0;
        if (page >= totalPages && totalElements > 0) page = totalPages - 1;

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<User> pagedStaff = staffOnlyList.subList(fromIndex, toIndex);

        // Danh sách ứng viên tiềm năng có thể thêm vào cửa hàng (chưa thuộc cửa hàng nào)
        List<User> allUsers = userRepository.findAll();
        List<User> candidateUsers = allUsers.stream()
                .filter(u -> u.getStore() == null && !u.getId().equals(user.getId())
                        && (shopOwner == null || !u.getId().equals(shopOwner.getId()))
                        && (u.getRole() == Role.STAFF || u.getRole() == Role.SHIPPER || u.getRole() == Role.CUSTOMER))
                .limit(20)
                .toList();

        model.addAttribute("store", store);
        model.addAttribute("shopOwner", shopOwner);
        model.addAttribute("staffList", pagedStaff);
        model.addAttribute("totalStaffCount", totalElements);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("activeTab", tab);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isPartner", isPartner);
        model.addAttribute("candidateUsers", candidateUsers);
        model.addAttribute("currentUser", user);

        return "manager/staff";
    }

    /**
     * Bước 13-18: Thêm nhân viên vào cửa hàng (Chỉ chủ cửa hàng có quyền)
     * Bước 15: Chọn các nhân viên muốn thêm (hỗ trợ chọn nhiều checkbox hoặc nhập username mới)
     * Bước 16: Bấm nút Submit -> Bước 17: Bấm nút Confirm -> Bước 18: Thông báo thành công
     */
    @PostMapping("/add")
    public String addStaff(Principal principal,
                           @RequestParam(value = "selectedUserIds", required = false) List<Long> selectedUserIds,
                           @RequestParam(value = "username", required = false) String username,
                           @RequestParam(value = "fullName", required = false) String fullName,
                           @RequestParam(value = "email", required = false) String email,
                           @RequestParam(value = "phone", required = false) String phone,
                           @RequestParam(value = "password", defaultValue = "123456") String password,
                           @RequestParam(value = "role", defaultValue = "STAFF") String roleStr,
                           RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireStore(store);

        boolean isOwner = (store.getOwner() != null && user.getId().equals(store.getOwner().getId()))
                || user.getRole() == Role.ADMIN;
        if (!isOwner) {
            throw new AccessDeniedException("Chỉ chủ cửa hàng mới có quyền thêm nhân viên.");
        }

        try {
            int addedCount = 0;

            // Cách 1: Chọn các nhân viên có sẵn trong hệ thống (Bước 15 dạng danh sách lựa chọn)
            if (selectedUserIds != null && !selectedUserIds.isEmpty()) {
                for (Long uid : selectedUserIds) {
                    var optUser = userRepository.findById(uid);
                    if (optUser.isPresent()) {
                        User u = optUser.get();
                        u.setStore(store);
                        if (u.getRole() == Role.CUSTOMER) {
                            u.setRole(Role.STAFF);
                        }
                        userRepository.save(u);
                        addedCount++;
                    }
                }
            }

            // Cách 2: Nhập username trực tiếp hoặc tạo tài khoản nhân viên mới
            if (username != null && !username.trim().isEmpty()) {
                String cleanUsername = username.trim();
                var existingOpt = userRepository.findByUsername(cleanUsername);
                if (existingOpt.isPresent()) {
                    User existing = existingOpt.get();
                    existing.setStore(store);
                    if (existing.getRole() == Role.CUSTOMER) {
                        existing.setRole(Role.STAFF);
                    }
                    userRepository.save(existing);
                    addedCount++;
                } else {
                    Role role = "SHIPPER".equalsIgnoreCase(roleStr) ? Role.SHIPPER : Role.STAFF;
                    User newStaff = User.builder()
                            .username(cleanUsername)
                            .fullName(fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : cleanUsername)
                            .email(email != null && !email.trim().isEmpty() ? email.trim() : cleanUsername + "@bloomflower.vn")
                            .phone(phone)
                            .password(passwordEncoder.encode(password))
                            .role(role)
                            .store(store)
                            .isEmailActive(true)
                            .isActive(true)
                            .build();
                    userRepository.save(newStaff);
                    addedCount++;
                }
            }

            if (addedCount > 0) {
                ra.addFlashAttribute("successMessage", "Đã thêm " + addedCount + " nhân viên vào cửa hàng thành công!");
            } else {
                ra.addFlashAttribute("errorMessage", "Vui lòng chọn hoặc điền thông tin nhân viên cần thêm!");
            }
        } catch (Exception ex) {
            log.error("Lỗi thêm nhân viên", ex);
            ra.addFlashAttribute("errorMessage", "Thêm nhân viên thất bại: " + ex.getMessage());
        }

        return "redirect:/manager/staff?tab=staffs";
    }

    /**
     * Alternative Flow 13-15a: Nút Delete nhân viên (Chỉ chủ cửa hàng có quyền)
     */
    @PostMapping({"/remove/{id}", "/delete/{id}"})
    public String deleteStaff(@PathVariable("id") Long id,
                              Principal principal,
                              RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireStore(store);

        boolean isOwner = (store.getOwner() != null && user.getId().equals(store.getOwner().getId()))
                || user.getRole() == Role.ADMIN;
        if (!isOwner) {
            throw new AccessDeniedException("Chỉ chủ cửa hàng mới có quyền xóa nhân viên khỏi cửa hàng.");
        }

        User staff = userRepository.findById(id).orElseThrow();
        if (staff.getStore() != null && staff.getStore().getId().equals(store.getId())) {
            staff.setStore(null);
            userRepository.save(staff);
            ra.addFlashAttribute("successMessage", "Đã xóa nhân viên [" + staff.getFullName() + "] khỏi cửa hàng thành công!");
        } else {
            ra.addFlashAttribute("errorMessage", "Nhân viên không thuộc cửa hàng này!");
        }

        return "redirect:/manager/staff?tab=staffs";
    }

    /**
     * Alternative Flow 13-15b: Nút Out this shop (Dành cho đối tác / partner rời khỏi cửa hàng)
     */
    @PostMapping("/out-shop")
    public String outThisShop(Principal principal, RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) return "redirect:/manager/stores";

        boolean isOwner = store.getOwner() != null && user.getId().equals(store.getOwner().getId());
        if (isOwner) {
            ra.addFlashAttribute("errorMessage", "Chủ cửa hàng không thể tự rời cửa hàng. Bạn cần chuyển nhượng hoặc xóa chi nhánh.");
            return "redirect:/manager/staff";
        }

        user.setStore(null);
        userRepository.save(user);

        ra.addFlashAttribute("successMessage", "Bạn đã rời khỏi cửa hàng [" + store.getName() + "] thành công!");
        return "redirect:/manager/stores";
    }

    private Store resolveStore(User user) {
        if (user.getStore() != null) {
            return user.getStore();
        }
        List<Store> owned = storeRepository.findByOwnerId(user.getId());
        if (!owned.isEmpty()) {
            Store first = owned.get(0);
            user.setStore(first);
            userRepository.save(user);
            return first;
        }
        return null;
    }
}
