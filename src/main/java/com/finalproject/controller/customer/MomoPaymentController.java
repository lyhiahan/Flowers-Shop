package com.finalproject.controller.customer;

import com.finalproject.entity.Order;
import com.finalproject.repository.OrderRepository;
import com.finalproject.service.MomoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MomoPaymentController {

    private final OrderRepository orderRepository;
    private final MomoService momoService;

    /**
     * Hiển thị giao diện thanh toán MoMo QR (Dùng khi quét mã hoặc khi MoMo Sandbox chuyển hướng về)
     */
    @GetMapping("/{orderId}")
    public String showMomoPaymentPage(@PathVariable("orderId") Long orderId,
                                      Principal principal,
                                      Model model) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));

        if (principal != null && !principal.getName().equals(order.getUser().getUsername())) {
            return "redirect:/customer/orders";
        }

        // Tạo dữ liệu mã QR MoMo
        long amount = order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0L;
        String momoQrData = "2|99|0909999888|Hoa Tuoi Sai Gon|info@flowershop.vn|0|0|" +
                amount + "|Thanh toan don hoa #" + order.getId() + "|transfer_mycode";

        model.addAttribute("order", order);
        model.addAttribute("amount", amount);
        model.addAttribute("momoQrData", momoQrData);

        return "customer/momo-payment";
    }

    /**
     * Xử lý Callback khi thanh toán MoMo thành công hoặc bị hủy
     */
    @GetMapping("/callback")
    public String handleMomoCallback(@RequestParam(value = "orderId", required = false) String momoOrderId,
                                     @RequestParam(value = "extraData", required = false) String extraData,
                                     @RequestParam(value = "resultCode", defaultValue = "0") int resultCode,
                                     @RequestParam(value = "transId", required = false, defaultValue = "") String transId,
                                     @RequestParam(value = "message", required = false) String message,
                                     RedirectAttributes ra) {
        log.info("Nhận callback MoMo: momoOrderId={}, extraData={}, resultCode={}, message={}", momoOrderId, extraData, resultCode, message);

        Long actualOrderId = null;
        if (extraData != null && !extraData.isBlank()) {
            try {
                actualOrderId = Long.parseLong(extraData.trim());
            } catch (Exception ignored) {}
        }
        if (actualOrderId == null && momoOrderId != null) {
            try {
                String cleanId = momoOrderId.contains("_") ? momoOrderId.split("_")[0] : momoOrderId;
                actualOrderId = Long.parseLong(cleanId);
            } catch (Exception ignored) {}
        }

        if (actualOrderId == null) {
            ra.addFlashAttribute("errorMessage", "Không xác định được mã đơn hàng từ giao dịch MoMo.");
            return "redirect:/customer/orders";
        }

        boolean success = momoService.processPaymentResult(actualOrderId, resultCode, transId);
        if (success) {
            ra.addFlashAttribute("successMessage", "🌸 Tuyệt vời! Bạn đã thanh toán thành công đơn hoa #" + actualOrderId + " qua Ví điện tử MoMo.");
            return "redirect:/customer/orders/detail/" + actualOrderId;
        } else {
            ra.addFlashAttribute("errorMessage", "Giao dịch MoMo chưa hoàn tất hoặc đã bị hủy. Đơn hàng vẫn được lưu lại để bạn thanh toán sau.");
            return "redirect:/customer/orders";
        }
    }

    /**
     * Webhook IPN xử lý ngầm từ máy chủ MoMo
     */
    @PostMapping("/ipn")
    @ResponseBody
    public ResponseEntity<?> handleMomoIpn(@RequestBody Map<String, Object> payload) {
        log.info("Nhận IPN từ máy chủ MoMo: {}", payload);
        try {
            String extraData = (String) payload.get("extraData");
            String momoOrderId = (String) payload.get("orderId");
            int resultCode = Integer.parseInt(String.valueOf(payload.getOrDefault("resultCode", -1)));
            String transId = String.valueOf(payload.getOrDefault("transId", ""));

            Long orderId = null;
            if (extraData != null && !extraData.isBlank()) {
                orderId = Long.parseLong(extraData.trim());
            } else if (momoOrderId != null) {
                String cleanId = momoOrderId.contains("_") ? momoOrderId.split("_")[0] : momoOrderId;
                orderId = Long.parseLong(cleanId);
            }

            if (orderId != null) {
                momoService.processPaymentResult(orderId, resultCode, transId);
            }
            return ResponseEntity.noContent().build(); // HTTP 204
        } catch (Exception e) {
            log.error("Lỗi xử lý IPN MoMo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Nút "Thanh toán lại qua MoMo" từ trang danh sách đơn của khách hàng
     */
    @GetMapping("/retry/{orderId}")
    public String retryPayment(@PathVariable("orderId") Long orderId,
                               Principal principal,
                               HttpServletRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));

        if (principal == null || !principal.getName().equals(order.getUser().getUsername())) {
            return "redirect:/customer/orders";
        }

        String paymentUrl = momoService.createPaymentUrl(order, request);
        return "redirect:" + paymentUrl;
    }
}
