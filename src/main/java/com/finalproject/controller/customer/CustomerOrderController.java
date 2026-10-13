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
    private final com.finalproject.repository.ReviewRepository reviewRepository;
    private final com.finalproject.repository.StoreRepository storeRepository;

    // UC_011 Main Flow 5 & 6: Hiển thị danh sách tất cả đơn và phân trang
    @GetMapping
    public String listCustomerOrders(Principal principal,
                                     @RequestParam(value = "status", required = false) String statusStr,
                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                     @RequestParam(value = "size", defaultValue = "5") int size,
                                     Model model) {
        model.addAttribute("stores", storeRepository.findByStatusTrue());
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
    // UC_014 Main Flow 3: Hiển thị trang chi tiết đơn hàng tương ứng
    @GetMapping({"/detail/{id}", "/{id}"})
    public String viewOrderDetail(Principal principal,
                                  @PathVariable("id") Long id,
                                  Model model) {
        com.finalproject.entity.Order order = orderService.getOrderById(id);
        if (!order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/customer/orders";
        }
        model.addAttribute("order", order);

        // Lấy danh sách sản phẩm đã được đánh giá trong đơn hàng này
        java.util.List<com.finalproject.entity.Review> reviews = reviewRepository.findByOrderId(id);
        java.util.Set<Long> reviewedProductIds = reviews.stream()
                .map(r -> r.getProduct().getId())
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("reviewedProductIds", reviewedProductIds);

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
            var order = orderService.cancelOrder(principal.getName(), id);
            String message = order.getStatus() == com.finalproject.enums.OrderStatus.REFUNDED
                    ? "Đã hủy đơn và hoàn tiền thành công." : "Đã hủy đơn hàng #" + id + " thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/orders";
    }
}
