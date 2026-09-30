package com.finalproject.controller.admin;

import com.finalproject.entity.Order;
import com.finalproject.entity.Store;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.StoreRepository;
import com.finalproject.service.OrderService;
import com.finalproject.service.OrderWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping({"/admin/orders", "/admin/orders-manage"})
@RequiredArgsConstructor
public class AdminOrderManagementController {

    private final OrderService orderService;
    private final OrderWorkflowService workflow;
    private final StoreRepository storeRepository;

    @GetMapping
    public String listAllOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(value = "status", required = false) String statusStr,
            Model model) {

        List<Order> allOrders = orderService.getAllOrders();

        // Đếm số lượng thực tế cho từng nhóm trạng thái
        long countAll = allOrders.size();
        long countPending = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long countConfirmed = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long countPreparing = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PREPARING).count();
        long countDelivering = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERING).count();
        long countDelivered = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long countCompleted = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        long countCancelled = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        List<Order> filtered = allOrders;

        // 1. Lọc theo trạng thái
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                OrderStatus st = OrderStatus.valueOf(statusStr.toUpperCase());
                filtered = filtered.stream().filter(o -> o.getStatus() == st).toList();
            } catch (Exception ignored) {}
        }

        // 2. Lọc theo từ khóa (Mã đơn, tên người nhận, SĐT, tên tài khoản đặt)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase().replace("#", "");
            filtered = filtered.stream().filter(o ->
                    String.valueOf(o.getId()).contains(kw)
                    || (o.getRecipientName() != null && o.getRecipientName().toLowerCase().contains(kw))
                    || (o.getRecipientPhone() != null && o.getRecipientPhone().contains(kw))
                    || (o.getUser() != null && o.getUser().getFullName() != null && o.getUser().getFullName().toLowerCase().contains(kw))
            ).toList();
        }

        // 3. Lọc theo Chi nhánh thực hiện
        if (storeId != null && storeId > 0) {
            filtered = filtered.stream().filter(o ->
                    o.getPickupStore() != null && storeId.equals(o.getPickupStore().getId())
            ).toList();
        }

        // 4. Lọc theo Lịch nhận hoa (deliveryDate hoặc ngày đặt)
        if (deliveryDate != null) {
            filtered = filtered.stream().filter(o ->
                    deliveryDate.equals(o.getDeliveryDate()) || (o.getDeliveryDate() == null && o.getCreatedAt() != null && deliveryDate.equals(o.getCreatedAt().toLocalDate()))
            ).toList();
        }

        List<Store> stores = storeRepository.findAll();

        model.addAttribute("orders", filtered);
        model.addAttribute("stores", stores);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentStoreId", storeId);
        model.addAttribute("currentDeliveryDate", deliveryDate);
        model.addAttribute("currentStatus", statusStr != null ? statusStr.toUpperCase() : "ALL");

        model.addAttribute("countAll", countAll);
        model.addAttribute("countPending", countPending);
        model.addAttribute("countConfirmed", countConfirmed);
        model.addAttribute("countPreparing", countPreparing);
        model.addAttribute("countDelivering", countDelivering);
        model.addAttribute("countDelivered", countDelivered);
        model.addAttribute("countCompleted", countCompleted);
        model.addAttribute("countCancelled", countCancelled);

        return "admin/orders-manage";
    }

    @PostMapping("/update-status/{id}")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") OrderStatus status,
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
                default -> status.name();
            };
            ra.addFlashAttribute("successMessage", "Đã cập nhật tiến độ đơn #" + id + " thành [" + statusLabel + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders-manage";
    }

    @PostMapping("/confirm-payment/{id}")
    public String confirmPayment(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            workflow.confirmPayment(id);
            ra.addFlashAttribute("successMessage", "Đã xác nhận thanh toán đơn #" + id);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders-manage";
    }
}
