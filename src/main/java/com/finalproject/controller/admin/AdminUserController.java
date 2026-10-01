package com.finalproject.controller.admin;

import com.finalproject.entity.RoleRequest;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.enums.RoleRequestStatus;
import com.finalproject.repository.RoleRequestRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.RoleManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final RoleRequestRepository roleRequestRepository;
    private final RoleManagementService roleManagementService;

    @GetMapping
    public String listUsers(@RequestParam(value = "tab", required = false, defaultValue = "users") String tab,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "role", required = false) String roleStr,
                            @RequestParam(value = "requestStatus", required = false) String requestStatusStr,
                            Model model) {
        // Tab 1: Danh sách người dùng
        List<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    keyword.trim(), keyword.trim(), keyword.trim());
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

        // Tab 2: Danh sách yêu cầu vai trò
        RoleRequestStatus statusFilter = null;
        if (requestStatusStr != null && !requestStatusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(requestStatusStr)) {
            try {
                statusFilter = RoleRequestStatus.valueOf(requestStatusStr.toUpperCase());
            } catch (Exception ignored) {}
        }
        List<RoleRequest> roleRequests = roleManagementService.getAllRoleRequests(statusFilter);
        long pendingCount = roleRequestRepository.countByStatus(RoleRequestStatus.PENDING);
        List<Store> stores = storeRepository.findAll();

        model.addAttribute("activeTab", tab);
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentRole", roleStr != null ? roleStr : "ALL");
        model.addAttribute("roles", Role.values());
        model.addAttribute("roleRequests", roleRequests);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("currentRequestStatus", requestStatusStr != null ? requestStatusStr : "ALL");
        model.addAttribute("stores", stores);

        return "admin/users";
    }

    @PostMapping("/toggle-active/{id}")
    public String toggleActive(@PathVariable("id") Long id, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElseThrow();
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            ra.addFlashAttribute("errorMessage", "Không thể khóa tài khoản Quản trị viên tối cao!");
            return "redirect:/admin/users?tab=users";
        }
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepository.save(user);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tài khoản [" + user.getUsername() + "] thành [" + (user.getIsActive() ? "Hoạt động" : "Bị khóa") + "]");
        return "redirect:/admin/users?tab=users";
    }

    @PostMapping("/role-request/approve/{id}")
    public String approveRoleRequest(@PathVariable("id") Long requestId,
                                     @RequestParam(value = "assignedStoreId", required = false) Long assignedStoreId,
                                     Principal principal,
                                     RedirectAttributes ra) {
        try {
            User admin = userRepository.findByUsername(principal.getName()).orElseThrow();
            RoleRequest approved = roleManagementService.approveRoleRequest(requestId, admin.getId(), assignedStoreId);
            ra.addFlashAttribute("successMessage", "Phê duyệt thành công yêu cầu vai trò [" + approved.getTargetRoleDisplayName()
                    + "] cho người dùng @" + approved.getUser().getUsername() + "!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Phê duyệt thất bại: " + ex.getMessage());
        }
        return "redirect:/admin/users?tab=requests";
    }

    @PostMapping("/role-request/reject/{id}")
    public String rejectRoleRequest(@PathVariable("id") Long requestId,
                                    @RequestParam("rejectionReason") String reason,
                                    Principal principal,
                                    RedirectAttributes ra) {
        try {
            User admin = userRepository.findByUsername(principal.getName()).orElseThrow();
            RoleRequest rejected = roleManagementService.rejectRoleRequest(requestId, admin.getId(), reason);
            ra.addFlashAttribute("successMessage", "Đã từ chối yêu cầu vai trò của @" + rejected.getUser().getUsername() + ". Người dùng vẫn giữ tài khoản Khách hàng bình thường.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Từ chối thất bại: " + ex.getMessage());
        }
        return "redirect:/admin/users?tab=requests";
    }

    @PostMapping("/assign-role/{id}")
    public String directlyAssignRole(@PathVariable("id") Long userId,
                                     @RequestParam("role") Role role,
                                     @RequestParam(value = "storeId", required = false) Long storeId,
                                     @RequestParam(value = "reason", required = false) String reason,
                                     Principal principal,
                                     RedirectAttributes ra) {
        try {
            User admin = userRepository.findByUsername(principal.getName()).orElseThrow();
            roleManagementService.directlyAssignRole(userId, role, storeId, reason, admin.getId());
            ra.addFlashAttribute("successMessage", "Đã gán vai trò [" + role.name() + "] cho người dùng thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi gán vai trò: " + ex.getMessage());
        }
        return "redirect:/admin/users?tab=users";
    }

    @PostMapping("/revoke-role/{id}")
    public String directlyRevokeRole(@PathVariable("id") Long userId,
                                     @RequestParam("role") Role role,
                                     @RequestParam(value = "reason", required = false) String reason,
                                     Principal principal,
                                     RedirectAttributes ra) {
        try {
            User admin = userRepository.findByUsername(principal.getName()).orElseThrow();
            roleManagementService.directlyRevokeRole(userId, role, reason, admin.getId());
            ra.addFlashAttribute("successMessage", "Đã thu hồi vai trò [" + role.name() + "] thành công! Tài khoản vẫn duy trì quyền Khách hàng hoạt động.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi thu hồi vai trò: " + ex.getMessage());
        }
        return "redirect:/admin/users?tab=users";
    }
}
