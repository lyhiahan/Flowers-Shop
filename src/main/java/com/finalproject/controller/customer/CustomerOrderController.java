package com.finalproject.controller.customer;

import com.finalproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    // UC_011 Main Flow 5 & 6: Hiển thị danh sách tất cả đơn và phân trang
    @GetMapping
    public String listCustomerOrders(Principal principal,
                                     @RequestParam(value = "status", required = false) String statusStr,
                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                     @RequestParam(value = "size", defaultValue = "5") int size,
                                     Model model) {
        com.finalproject.enums.OrderStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                status = com.finalproject.enums.OrderStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception ignored) {}
        }
        java.util.List<com.finalproject.entity.Order> allOrders = orderService.getOrdersByUsernameAndStatus(principal.getName(), status);
        int totalOrders = allOrders.size();
        int pageSize = Math.max(1, size);
        int totalPages = (int) Math.ceil((double) totalOrders / pageSize);
        if (totalPages == 0) totalPages = 1;

        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(currentPage * pageSize, totalOrders);
        int toIndex = Math.min(fromIndex + pageSize, totalOrders);
        java.util.List<com.finalproject.entity.Order> pagedOrders = allOrders.subList(fromIndex, toIndex);

        model.addAttribute("currentStatus", statusStr != null ? statusStr : "ALL");
        model.addAttribute("orders", pagedOrders);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("hasPrevious", currentPage > 0);
        model.addAttribute("hasNext", currentPage < totalPages - 1);
        return "customer/orders";
    }

    // UC_011 Main Flow 7 & 8: Xem chi tiết đơn hàng
    @GetMapping({"/detail/{id}", "/{id}"})
    public String viewOrderDetail(Principal principal,
                                  @PathVariable("id") Long id,
                                  Model model) {
        com.finalproject.entity.Order order = orderService.getOrderById(id);
        if (!order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/customer/orders";
        }
        model.addAttribute("order", order);
        return "customer/order-detail";
    }

    @PostMapping("/refund/{id}")
    public String handleRefundOrder(Principal principal,
                                    @PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.requestRefund(principal.getName(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu Trả hàng - Hoàn tiền cho đơn #" + id + " đã được gửi đến shop!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/orders";
    }

    // UC_011 Alternative 9a: Bấm nút Confirm -> Hủy đơn hàng thành công
    @PostMapping("/cancel/{id}")
    public String handleCancelOrder(Principal principal,
                                    @PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(principal.getName(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + id + " thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/orders";
    }
}