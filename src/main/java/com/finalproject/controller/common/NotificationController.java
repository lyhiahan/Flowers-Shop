package com.finalproject.controller.common;

import com.finalproject.entity.Notification;
import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.NotificationService;
import com.finalproject.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final StoreAccessService storeAccess;
    private final com.finalproject.service.NotificationNavigationResolver navigationResolver;

    /**
     * Trang xem toàn bộ thông báo của người dùng (tự động hiển thị layout theo vai trò: Admin -> /admin/notifications, Manager -> /manager/notifications, Customer -> customer/notifications)
     */
    @GetMapping({"/notifications", "/manager/notifications"})
    public String viewAllNotifications(jakarta.servlet.http.HttpServletRequest request, Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        Role effectiveRole = navigationResolver.resolveRole(user);
        if (effectiveRole == Role.ADMIN) {
            return "redirect:/admin/notifications";
        }

        if (effectiveRole == Role.MANAGER) {
            if (request != null && "/notifications".equals(request.getRequestURI())) {
                return "redirect:/manager/notifications";
            }
            List<Notification> notifications = notificationService.getUserNotifications(user.getId());
            long unreadCount = notificationService.getUnreadCount(user.getId());

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("user", user);
            try {
                if (storeAccess != null) {
                    model.addAttribute("managerStore", storeAccess.selectedStore());
                }
            } catch (Exception ignored) {}
            return "manager/notifications";
        }

        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        long unreadCount = notificationService.getUnreadCount(user.getId());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("user", user);

        return "customer/notifications";
    }

    public String viewAllNotifications(Principal principal, Model model) {
        return viewAllNotifications(null, principal, model);
    }

    /**
     * Trang xem toàn bộ thông báo dành riêng cho Quản trị viên hệ thống
     */
    @GetMapping("/admin/notifications")
    public String viewAdminNotifications(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        long unreadCount = notificationService.getUnreadCount(user.getId());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("user", user);
        model.addAttribute("active", "notifications");

        return "admin/notifications";
    }

    /**
     * API lấy số lượng thông báo chưa đọc và thông tin người dùng cho chuông thông báo & WebSocket
     */
    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount(Principal principal) {
        if (principal == null) return ResponseEntity.ok(Map.of("unreadCount", 0L));
        return userRepository.findByUsername(principal.getName())
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("unreadCount", notificationService.getUnreadCount(u.getId()));
                    map.put("userId", u.getId());
                    map.put("username", u.getUsername());
                    Role effectiveRole = navigationResolver.resolveRole(u);
                    map.put("isManager", effectiveRole == Role.MANAGER);
                    map.put("isAdmin", effectiveRole == Role.ADMIN);
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.ok(Map.of("unreadCount", 0L)));
    }

    /**
     * API lấy 6 thông báo mới nhất cho dropdown chuông thông báo
     */
    @GetMapping("/api/notifications/latest")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getLatestNotifications(Principal principal) {
        if (principal == null) return ResponseEntity.ok(List.of());

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.ok(List.of());

        List<Notification> list = notificationService.getUserNotifications(user.getId());
        List<Map<String, Object>> result = list.stream().limit(6).map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("type", n.getType());
            map.put("isRead", n.getIsRead());
            map.put("relatedOrderId", n.getRelatedOrderId());
            map.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
            String targetUrl = n.getTargetUrl() != null ? n.getTargetUrl()
                    : (navigationResolver != null ? navigationResolver.resolveTargetUrl(user, n.getId(), n.getRelatedOrderId()) : "");
            map.put("targetUrl", targetUrl);
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Đánh dấu 1 thông báo là đã đọc
     */
    @PostMapping("/api/notifications/read/{id}")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        userRepository.findByUsername(principal.getName()).ifPresent(u -> {
            notificationService.markAsRead(id, u.getId());
        });
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    @PostMapping("/api/notifications/read-all")
    @ResponseBody
    public ResponseEntity<?> markAllAsRead(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        userRepository.findByUsername(principal.getName()).ifPresent(u -> {
            notificationService.markAllAsRead(u.getId());
        });
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
