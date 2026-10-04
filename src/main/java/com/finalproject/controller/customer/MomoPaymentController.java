package com.finalproject.controller.customer;

import com.finalproject.config.MomoConfig;
import com.finalproject.entity.Order;
import com.finalproject.repository.OrderRepository;
import com.finalproject.service.MomoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/checkout/momo")
@RequiredArgsConstructor
public class MomoPaymentController {
    private final OrderRepository orders;
    private final MomoService momo;
    private final MomoConfig config;

    private Order ownedOrder(Long id, Principal principal) {
        Order order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (principal == null || !principal.getName().equals(order.getUser().getUsername()))
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thanh toán đơn này.");
        return order;
    }

    @GetMapping("/{orderId}")
    public String page(@PathVariable Long orderId, Principal principal, Model model) {
        model.addAttribute("order", ownedOrder(orderId, principal));
        model.addAttribute("mockMode", config.isMock());
        return "customer/momo-payment";
    }

    // Browser redirects are informational; only verified IPN settles a sandbox payment.
    @GetMapping("/callback")
    public String callback(RedirectAttributes ra) {
        ra.addFlashAttribute("successMessage", "Kết quả MoMo sẽ cập nhật khi hệ thống nhận xác nhận hợp lệ. Bạn có thể kiểm tra trạng thái trong danh sách đơn.");
        return "redirect:/customer/orders";
    }

    @PostMapping("/ipn") @ResponseBody
    public ResponseEntity<?> ipn(@RequestBody Map<String, Object> payload) {
        try {
            momo.processNotification(payload);
            return ResponseEntity.noContent().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(Map.of("message", "Kết quả MoMo chưa được xác thực."));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Kết quả MoMo không khớp giao dịch đang chờ."));
        }
    }

    // Local demo requires the order owner, session, CSRF and mock mode.
    @PostMapping("/{orderId}/simulate")
    public String simulate(@PathVariable Long orderId, Principal principal, RedirectAttributes ra) {
        ownedOrder(orderId, principal);
        try {
            momo.simulatePayment(orderId, principal.getName());
            ra.addFlashAttribute("successMessage", "[DEV] Đã mô phỏng thanh toán thành công. Không có tiền thật được chuyển.");
        } catch (IllegalArgumentException | IllegalStateException ex) { ra.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/customer/orders/detail/" + orderId;
    }

    @PostMapping("/retry/{orderId}")
    public String retry(@PathVariable Long orderId, Principal principal, HttpServletRequest request, RedirectAttributes ra) {
        Order order = ownedOrder(orderId, principal);
        try { return "redirect:" + momo.createPaymentUrl(order, request); }
        catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/orders";
        }
    }
}
