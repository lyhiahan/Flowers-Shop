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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final RoleRequestRepository roleRequestRepository;
    private final RoleManagementService roleManagementService;
    private final com.finalproject.repository.OrderRepository orderRepository;
    private final com.finalproject.repository.CartRepository cartRepository;
    private final com.finalproject.repository.CartItemRepository cartItemRepository;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @GetMapping
    public String listUsers(@RequestParam(value = "tab", required = false, defaultValue = "users") String tab,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "role", required = false) String roleStr,
                            @RequestParam(value = "requestStatus", required = false) String requestStatusStr,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            Model model) {
        try {
            // Tab 1: Danh sách người dùng có phân trang (UC_027)
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "id"));
            Page<User> userPage;
            if (keyword != null && !keyword.trim().isEmpty()) {
                userPage = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), keyword.trim(), pageable);
            } else if (roleStr != null && !roleStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(roleStr)) {
                try {
                    Role role = Role.valueOf(roleStr.toUpperCase());
                    userPage = userRepository.findByRole(role, pageable);
                } catch (Exception ex) {
                    userPage = userRepository.findAll(pageable);
                }
            } else {
                userPage = userRepository.findAll(pageable);
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
            model.addAttribute("users", userPage.getContent());
            model.addAttribute("userPage", userPage);
            model.addAttribute("currentPage", userPage.getNumber());
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("totalElements", userPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentRole", roleStr != null ? roleStr : "ALL");
            model.addAttribute("roles", Role.values());
            model.addAttribute("roleRequests", roleRequests);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("currentRequestStatus", requestStatusStr != null ? requestStatusStr : "ALL");
            model.addAttribute("stores", stores);
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

    @PostMapping("/delete/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteUser(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy người dùng #" + id);
                return "redirect:/admin/users?tab=users";
            }

            // 1. Chặn xóa admin tối cao hoặc chính mình
            if ("admin".equalsIgnoreCase(user.getUsername()) || (user.getRole() != null && user.getRole() == Role.ADMIN)) {
                ra.addFlashAttribute("errorMessage", "Không thể xóa tài khoản Quản trị viên tối cao!");
                return "redirect:/admin/users?tab=users";
            }
            if (principal != null && principal.getName().equalsIgnoreCase(user.getUsername())) {
                ra.addFlashAttribute("errorMessage", "Bạn không thể tự xóa tài khoản của chính mình khi đang đăng nhập!");
                return "redirect:/admin/users?tab=users";
            }

            // 2. Chặn xóa nếu là chủ cửa hàng
            if (!storeRepository.findByOwnerId(id).isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Không thể xóa: Người dùng đang là Chủ sở hữu chi nhánh/cửa hàng. Vui lòng chuyển giao hoặc xóa cửa hàng trước!");
                return "redirect:/admin/users?tab=users";
            }

            // 3. Chặn xóa nếu đã có đơn hàng (để bảo toàn hóa đơn và kế toán)
            boolean hasOrders = !orderRepository.findByUserIdOrderByCreatedAtDesc(id).isEmpty()
                    || orderRepository.existsByLegacyDeliveryContactId(id);
            if (hasOrders) {
                ra.addFlashAttribute("errorMessage", "Tài khoản [" + user.getUsername() + "] đã phát sinh đơn hàng trong hệ thống. Nhằm bảo toàn chứng từ giao dịch, vui lòng sử dụng chức năng [Khóa] tài khoản thay vì xóa!");
                return "redirect:/admin/users?tab=users";
            }

            String username = user.getUsername();
            String email = user.getEmail();

            // 4. Xóa giỏ hàng và các phụ thuộc
            cartRepository.findByUserId(id).ifPresent(c -> {
                if (c.getItems() != null && !c.getItems().isEmpty()) {
                    cartItemRepository.deleteAll(c.getItems());
                }
                cartRepository.delete(c);
            });

            // Xóa các bảng phụ thuộc an toàn bằng native queries
            entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_addresses WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM product_wishlists WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM recently_viewed WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM notifications WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM role_requests WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM role_audit_logs WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM otp_verifications WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_follow_store WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM activity_logs WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM reviews WHERE user_id = :uid").setParameter("uid", id).executeUpdate();

            // Xóa người dùng khỏi database
            userRepository.delete(user);

            ra.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn tài khoản [" + username + "] và giải phóng email [" + email + "] thành công! Bây giờ có thể dùng email này để đăng ký tài khoản mới.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa tài khoản: " + ex.getMessage());
        }
        return "redirect:/admin/users?tab=users";
    }
}
