package com.finalproject.controller.manager;

import com.finalproject.entity.Order;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.OrderService;
import com.finalproject.service.OrderWorkflowService;
import com.finalproject.service.StoreAccessService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

/**
 * Controller triển khai Use Case UC_022: Quản lý đơn hàng của cửa hàng (Store Management – Order)
 */
@Controller
@RequestMapping({"/manager/orders", "/vendor/orders"})
@RequiredArgsConstructor
public class ManagerOrderController {
    private final com.finalproject.service.OrderAftercareService aftercare;

    private final OrderService orderService;
    private final OrderWorkflowService workflow;
    private final UserRepository userRepository;
    private final StoreAccessService storeAccess;
    private final com.finalproject.repository.OrderRepository orders;

    private static final Set<OrderStatus> PROCESSING_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.PICKED_UP,
            OrderStatus.DELIVERING, OrderStatus.DELIVERED, OrderStatus.REFUND_REQUESTED
    );

    private static final Set<OrderStatus> PROCESSED_STATUSES = Set.of(
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED,
            OrderStatus.REFUNDED
    );

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusOption {
        private String value;
        private String label;
    }

    /**
     * Bước 7 & 8: Chuyển đến trang quản lý đơn hàng cửa hàng
     * Bước 8: Chọn Processing orders (hoặc Processed orders) để xem bảng danh sách các đơn hàng đang xử lý (đã xử lý)
     * Bước 9: Phân trang (Next, Previous, hoặc số trang bất kỳ)
     */
    @GetMapping
    public String listOrders(Principal principal,
                             @RequestParam(value = "tab", required = false) String tab,
                             @RequestParam(value = "status", required = false) String statusStr,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "8") int size,
                             Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) {
            return "redirect:/manager/stores";
        }
        storeAccess.requireManager(store);

        org.springframework.data.jpa.domain.Specification<Order> scope = (root, query, cb) -> cb.equal(root.get("pickupStore").get("id"), store.getId());
        long processingCount = orders.count(scope.and((root, query, cb) -> root.get("status").in(PROCESSING_STATUSES)));
        long processedCount = orders.count(scope.and((root, query, cb) -> root.get("status").in(PROCESSED_STATUSES)));
        long totalCount = orders.count(scope);

        // Xác định active tab: processing, processed, hoặc all
        String activeTab = (tab != null && !tab.trim().isEmpty()) ? tab.toLowerCase() : "all";
        if ("PROCESSING".equalsIgnoreCase(statusStr)) activeTab = "processing";
        if ("PROCESSED".equalsIgnoreCase(statusStr)) activeTab = "processed";

        if (!Set.of("all", "processing", "processed").contains(activeTab)) throw new IllegalArgumentException("Tab đơn hàng không hợp lệ.");
        org.springframework.data.jpa.domain.Specification<Order> filter = scope;
        if ("processing".equals(activeTab)) filter = filter.and((root, query, cb) -> root.get("status").in(PROCESSING_STATUSES));
        else if ("processed".equals(activeTab)) filter = filter.and((root, query, cb) -> root.get("status").in(PROCESSED_STATUSES));
        else if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            OrderStatus wanted = OrderStatus.valueOf(statusStr.toUpperCase());
            filter = filter.and((root, query, cb) -> cb.equal(root.get("status"), wanted));
        }
        var finalFilter = filter;
        var result = com.finalproject.utils.StorePages.load(page, size, pageable -> orders.findAll(finalFilter, pageable));
        List<Order> pagedOrders = result.getContent();
        com.finalproject.utils.StorePages.model(model, result);

        model.addAttribute("orders", pagedOrders);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("currentStatus", statusStr != null ? statusStr : "ALL");
        model.addAttribute("processingCount", processingCount);
        model.addAttribute("processedCount", processedCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("store", store);
        model.addAttribute("currentUser", user);

        return "manager/orders";
    }

    /**
     * Bước 10 & 11: Chọn nút xem chi tiết -> Chuyển đến trang chi tiết đơn hàng
     * Bước 12: Chọn cập nhật trạng thái đơn hàng ở phần dropdown (nút này sẽ tồn tại với các đơn hàng đang xử lý)
     */
    @GetMapping({"/{id}", "/detail/{id}"})
    public String orderDetail(@PathVariable("id") Long id,
                              Principal principal,
                              Model model,
                              RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = resolveStore(user);
        if (store == null) {
            return "redirect:/manager/stores";
        }
        storeAccess.requireManager(store);

        Order order;
        try {
            order = orderService.getOrderById(id);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/manager/orders";
        }

        if (user.getRole() != Role.ADMIN && (order.getPickupStore() == null || !store.getId().equals(order.getPickupStore().getId()))) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền xem đơn hàng của chi nhánh khác.");
            return "redirect:/manager/orders";
        }

        boolean isProcessing = PROCESSING_STATUSES.contains(order.getStatus());
        List<StatusOption> availableStatuses = getAvailableStatuses(order);

        model.addAttribute("order", order);
        model.addAttribute("store", store);
        model.addAttribute("currentUser", user);
        model.addAttribute("isProcessing", isProcessing);
        model.addAttribute("availableStatuses", availableStatuses);
        aftercare.addToModel(order, model);

        return "manager/order-detail";
    }

    /**
     * Bước 13: Hiển thị thông báo xác nhận
     * Bước 14: Bấm nút Confirm
     * Bước 15: Thông báo thành công
     * 14a: Bấm nút Cancel
     * 15a: Hủy thao tác
     */
    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") OrderStatus status,
                               @RequestParam(value = "cancellationReason", required = false) String cancellationReason,
                               @RequestParam(value = "redirect", required = false) String redirectUrl,
                               RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, status, cancellationReason);
            OrderStatus updatedStatus = orderService.getOrderById(id).getStatus();
            ra.addFlashAttribute("successMessage", "Đã cập nhật đơn #" + id + " sang trạng thái “" + updatedStatus.getDisplayName() + "”.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (com.finalproject.utils.SafeRedirect.isWithin(redirectUrl, "/manager/orders", "/vendor/orders")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/manager/orders/" + id;
    }

    @PostMapping("/confirm-payment/{id}")
    public String confirmPayment(@PathVariable("id") Long id,
                                 @RequestParam(value = "redirect", required = false) String redirectUrl,
                                 RedirectAttributes ra) {
        try {
            workflow.confirmPayment(id);
            ra.addFlashAttribute("successMessage", "Đã xác nhận nhận thanh toán cho đơn #" + id);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (com.finalproject.utils.SafeRedirect.isWithin(redirectUrl, "/manager/orders", "/vendor/orders")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/manager/orders";
    }

    private List<StatusOption> getAvailableStatuses(Order order) {
        return workflow.availableTransitions(order).stream()
                .map(next -> new StatusOption(next.name(), workflow.transitionLabel(order, next))).toList();
    }

    private Store resolveStore(User user) {
        return storeAccess.selectedStore();
    }
}
