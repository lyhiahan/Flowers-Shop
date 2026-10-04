package com.finalproject.controller.admin;

import com.finalproject.entity.Order;
import com.finalproject.entity.Store;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.StoreRepository;
import com.finalproject.service.OrderService;
import com.finalproject.service.OrderWorkflowService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping({"/admin/orders", "/admin/orders-manage"})
@RequiredArgsConstructor
public class AdminOrderManagementController {

    private final OrderService orderService;
    private final OrderWorkflowService workflow;
    private final StoreRepository storeRepository;

    @Getter
    public static class StatusOption {
        private final String value;
        private final String label;

        public StatusOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }

    /**
     * UC_029 Bước 1 - 5: Hiển thị bảng danh sách đơn hàng với bộ lọc, tabs và phân trang
     */
    @GetMapping
    public String listAllOrders(
            @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(value = "status", required = false) String statusStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        try {
            List<Order> allOrders = orderService.getAllOrders();

            // Đếm số lượng thực tế cho từng nhóm trạng thái
            long countAll = allOrders.size();
            long countDeliveryService = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERING || o.getStatus() == OrderStatus.PICKED_UP).count();
            long countPending = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
            long countConfirmed = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
            long countPreparing = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PREPARING).count();
            long countDelivering = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERING).count();
            long countDelivered = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
            long countCompleted = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
            long countCancelled = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

            List<Order> filtered = allOrders;

            // BƯỚC 3 THEO UC_029: Chọn All (hoặc Delivery Service) để xem tất cả (hoặc các đơn hàng đang vận chuyển)
            if ("DELIVERY_SERVICE".equalsIgnoreCase(tab) || "DELIVERY_SERVICE".equalsIgnoreCase(statusStr)) {
                filtered = filtered.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERING || o.getStatus() == OrderStatus.PICKED_UP).toList();
            } else if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
                try {
                    OrderStatus st = OrderStatus.valueOf(statusStr.toUpperCase());
                    filtered = filtered.stream().filter(o -> o.getStatus() == st).toList();
                } catch (Exception ignored) {}
            }

            // Lọc theo từ khóa (Mã đơn, tên người nhận, SĐT, tên tài khoản đặt)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase().replace("#", "");
                filtered = filtered.stream().filter(o ->
                        String.valueOf(o.getId()).contains(kw)
                        || (o.getRecipientName() != null && o.getRecipientName().toLowerCase().contains(kw))
                        || (o.getRecipientPhone() != null && o.getRecipientPhone().contains(kw))
                        || (o.getUser() != null && o.getUser().getFullName() != null && o.getUser().getFullName().toLowerCase().contains(kw))
                ).toList();
            }

            // Lọc theo Chi nhánh thực hiện
            if (storeId != null && storeId > 0) {
                filtered = filtered.stream().filter(o ->
                        o.getPickupStore() != null && storeId.equals(o.getPickupStore().getId())
                ).toList();
            }

            // Lọc theo Lịch nhận hoa (deliveryDate hoặc ngày đặt)
            if (deliveryDate != null) {
                filtered = filtered.stream().filter(o ->
                        deliveryDate.equals(o.getDeliveryDate()) || (o.getDeliveryDate() == null && o.getCreatedAt() != null && deliveryDate.equals(o.getCreatedAt().toLocalDate()))
                ).toList();
            }

            // BƯỚC 5 THEO UC_029: Phân trang danh sách đơn hàng
            int totalElements = filtered.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            if (totalPages == 0) totalPages = 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            List<Order> pagedOrders = filtered.subList(fromIndex, toIndex);

            List<Store> stores = storeRepository.findAll();

            model.addAttribute("orders", pagedOrders);
            model.addAttribute("stores", stores);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentStoreId", storeId);
            model.addAttribute("currentDeliveryDate", deliveryDate);
            model.addAttribute("currentStatus", statusStr != null ? statusStr.toUpperCase() : "ALL");
            model.addAttribute("currentTab", tab != null ? tab.toUpperCase() : "ALL");

            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", (long) totalElements);
            model.addAttribute("pageSize", size);

            model.addAttribute("countAll", countAll);
            model.addAttribute("countDeliveryService", countDeliveryService);
            model.addAttribute("countPending", countPending);
            model.addAttribute("countConfirmed", countConfirmed);
            model.addAttribute("countPreparing", countPreparing);
            model.addAttribute("countDelivering", countDelivering);
            model.addAttribute("countDelivered", countDelivered);
            model.addAttribute("countCompleted", countCompleted);
            model.addAttribute("countCancelled", countCancelled);
            model.addAttribute("dataLoadError", false);

        } catch (Exception ex) {
            model.addAttribute("dataLoadError", true);
            model.addAttribute("errorMessage", "Không thể tải danh sách đơn hàng. Vui lòng thử lại!");
            model.addAttribute("orders", Collections.emptyList());
            model.addAttribute("stores", Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("pageSize", size);
            model.addAttribute("countAll", 0L);
            model.addAttribute("countDeliveryService", 0L);
        }

        return "admin/orders-manage";
    }

    /**
     * UC_029 Bước 6 & 7: Xem chi tiết đơn hàng
     * Bước 8: Kiểm tra đơn hàng đã bàn giao cho đơn vị vận chuyển (isHandedOverToDelivery)
     */
    @GetMapping({"/{id}", "/detail/{id}"})
    public String viewOrderDetail(@PathVariable("id") Long id, Model model) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return "redirect:/admin/orders-manage";
        }

        // BƯỚC 8 THEO UC_029: Kiểm tra nếu đơn hàng đã bàn giao cho đơn vị vận chuyển
        boolean isHandedOverToDelivery = order.getStatus() != null && (order.getStatus() == OrderStatus.DELIVERING || order.getStatus() == OrderStatus.PICKED_UP);

        List<StatusOption> availableStatuses = workflow.availableTransitions(order).stream()
                .map(next -> new StatusOption(next.name(), workflow.transitionLabel(order, next))).toList();

        model.addAttribute("order", order);
        model.addAttribute("isHandedOverToDelivery", isHandedOverToDelivery);
        model.addAttribute("availableStatuses", availableStatuses);

        return "admin/order-detail";
    }

    /**
     * UC_029 Bước 8 - 11 & Alternative 10a, 11a:
     * Cập nhật trạng thái đơn hàng (Confirm / Cancel)
     */
    @PostMapping("/update-status/{id}")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") OrderStatus status,
                                    @RequestParam(value = "redirect", required = false) String redirectUrl,
                                    RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, status);
            String statusLabel = switch (status) {
                case CONFIRMED -> "Đã tiếp nhận";
                case PREPARING -> "Đang cắm hoa";
                case DELIVERING -> "Đang giao";
                case DELIVERED -> "Đã giao tới";
                case COMPLETED -> "Hoàn thành";
                case CANCELLED -> "Đã hủy";
                case REFUNDED -> "Đã xác nhận hoàn tiền";
                default -> status.name();
            };
            if (status == OrderStatus.CANCELLED && orderService.getOrderById(id).getStatus() == OrderStatus.REFUND_REQUESTED) {
                statusLabel = "Đã hủy, chờ hoàn tiền — vui lòng hoàn tiền lại cho khách hàng";
            }
            ra.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn #" + id + " thành [" + statusLabel + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (com.finalproject.utils.SafeRedirect.isWithin(redirectUrl, "/admin/orders")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/admin/orders-manage/" + id;
    }

    @PostMapping("/confirm-payment/{id}")
    public String confirmPayment(@PathVariable("id") Long id,
                                 @RequestParam(value = "redirect", required = false) String redirectUrl,
                                 RedirectAttributes ra) {
        try {
            workflow.confirmPayment(id);
            ra.addFlashAttribute("successMessage", "Đã xác nhận thanh toán đơn #" + id);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (com.finalproject.utils.SafeRedirect.isWithin(redirectUrl, "/admin/orders-manage", "/admin/orders")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/admin/orders-manage";
    }
}

