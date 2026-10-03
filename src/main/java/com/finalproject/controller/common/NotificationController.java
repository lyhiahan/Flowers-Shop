package com.finalproject.controller.common;

import com.finalproject.entity.Notification;
import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.NotificationService;
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

    /**
     * Trang xem toàn bộ thông báo của người dùng
     */
    @GetMapping("/notifications")
    public String viewAllNotifications(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        long unreadCount = notificationService.getUnreadCount(user.getId());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("user", user);

        return "customer/notifications";
    }

    /**
     * API lấy số lượng thông báo chưa đọc cho chuông thông báo (Bell Badge)
     */
    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount(Principal principal) {
        if (principal == null) return ResponseEntity.ok(Map.of("unreadCount", 0));
        return userRepository.findByUsername(principal.getName())
                .map(u -> ResponseEntity.ok(Map.<String, Object>of("unreadCount", notificationService.getUnreadCount(u.getId()))))
                .orElse(ResponseEntity.ok(Map.of("unreadCount", 0)));
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
