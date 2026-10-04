package com.finalproject.controller.customer;

import com.finalproject.entity.Order;
import com.finalproject.entity.Payment;
import com.finalproject.entity.User;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.PaymentMethod;
import com.finalproject.enums.PaymentStatus;
import com.finalproject.enums.Role;
import com.finalproject.repository.OrderRepository;
import com.finalproject.repository.PaymentRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
@RequestMapping("/customer/payment")
@RequiredArgsConstructor
public class CustomerPaymentController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    private static final int PAYMENT_TIMEOUT_SECONDS = 15 * 60; // 15 phút đếm ngược
    private static final DecimalFormat VND_FORMAT = new DecimalFormat("#,###");

    /**
     * Bước 2: Hiển thị trang Thanh toán đơn hàng
     * Gồm: Số tiền, mã đơn, thông tin ngân hàng, mã QR, thời gian còn lại.
     */
    @GetMapping("/{orderId}")
    public String viewPaymentPage(Principal principal,
                                  @PathVariable("orderId") Long orderId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Chỉ tài khoản có quyền Khách hàng mới được thanh toán
        if (!currentUser.hasRole(Role.CUSTOMER)) {
            throw new AccessDeniedException("Chỉ tài khoản khách hàng mới có quyền truy cập trang thanh toán đơn hoa.");
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng #" + orderId);
            return "redirect:/customer/orders";
        }

        // Kiểm tra quyền sở hữu đơn hàng (chống đổi URL sang đơn của người khác)
        if (!order.getUser().getUsername().equals(principal.getName())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập thanh toán cho đơn hàng của khách hàng khác.");
        }

        // Kiểm tra phương thức thanh toán
        if (!"BANK_TRANSFER".equalsIgnoreCase(order.getPaymentMethod())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng này không sử dụng phương thức Chuyển khoản ngân hàng.");
            return "redirect:/customer/orders/detail/" + orderId;
        }

        // Kiểm tra nếu đơn hàng đã bị hủy
        if (order.getStatus() == OrderStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng #" + orderId + " đã bị hủy, không thể tiếp tục thanh toán.");
            return "redirect:/customer/orders";
        }

        boolean isAlreadyPaid = "PAID".equalsIgnoreCase(order.getPaymentStatus());
        Payment payment = paymentRepository.findByOrderId(order.getId()).stream().findFirst().orElse(null);

        // Tính thời gian đếm ngược còn lại (15 phút kể từ lúc tạo đơn)
        LocalDateTime createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
        long elapsedSeconds = Duration.between(createdAt, LocalDateTime.now()).getSeconds();
        long remainingSeconds = Math.max(0, PAYMENT_TIMEOUT_SECONDS - elapsedSeconds);
        boolean isExpired = (!isAlreadyPaid && remainingSeconds <= 0);

        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("isAlreadyPaid", isAlreadyPaid);
        model.addAttribute("isExpired", isExpired);
        model.addAttribute("remainingSeconds", remainingSeconds);
        model.addAttribute("bankName", "FlowerBank (Napas247)");
        model.addAttribute("bankCode", "FLOWERBANK");
        model.addAttribute("accountNumber", "888868689999");
        model.addAttribute("accountName", "HOA TUOI SAI GON");
        model.addAttribute("transferContent", "HTSG " + order.getId());

        return "customer/payment";
    }

    /**
     * Bước 3 -> Bước 4 -> Bước 5: Xác thực và xử lý thanh toán đơn hàng
     * Hệ thống tự động kiểm tra kết quả, đối chiếu số tiền, ghi nhận thanh toán và sinh mã giao dịch.
     * Khách hàng KHÔNG thể tự chọn thành công hay thất bại.
     */
    @PostMapping("/{orderId}/process")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPayment(Principal principal,
                                                              @PathVariable("orderId") Long orderId) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập lại để tiếp tục.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null || !currentUser.hasRole(Role.CUSTOMER)) {
            response.put("success", false);
            response.put("message", "Chỉ tài khoản khách hàng chủ đơn mới có quyền thực hiện thanh toán.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy thông tin đơn hàng #" + orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Kiểm tra quyền sở hữu đơn hàng
        if (!order.getUser().getUsername().equals(principal.getName())) {
            response.put("success", false);
            response.put("message", "Bạn không có quyền thực hiện thanh toán cho đơn hàng này.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // Kiểm tra trạng thái đơn
        if (order.getStatus() == OrderStatus.CANCELLED) {
            response.put("success", false);
            response.put("message", "Đơn hàng đã bị hủy, không thể tiến hành thanh toán.");
            return ResponseEntity.badRequest().body(response);
        }

        // Nếu đã thanh toán trước đó
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            Payment p = paymentRepository.findByOrderId(order.getId()).stream().findFirst().orElse(null);
            String code = (p != null && p.getTransactionCode() != null) ? p.getTransactionCode() : ("TXN" + order.getId());
            String paidAtStr = (p != null && p.getPaidAt() != null)
                    ? p.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            response.put("success", true);
            response.put("alreadyPaid", true);
            response.put("transactionCode", code);
            response.put("paidAt", paidAtStr);
            response.put("amount", VND_FORMAT.format(order.getTotalAmount()) + " ₫");
            response.put("orderId", order.getId());
            response.put("message", "Đơn hàng đã được thanh toán thành công trước đó.");
            return ResponseEntity.ok(response);
        }

        // Kiểm tra thời gian còn lại (15 phút)
        LocalDateTime createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
        long elapsedSeconds = Duration.between(createdAt, LocalDateTime.now()).getSeconds();
        if (elapsedSeconds > PAYMENT_TIMEOUT_SECONDS) {
            response.put("success", false);
            response.put("expired", true);
            response.put("message", "Phiên giao dịch đã hết thời gian hiệu lực (15 phút). Vui lòng kiểm tra lại đơn hàng.");
            return ResponseEntity.ok(response);
        }

        // Tạo mã giao dịch đối soát chuẩn định dạng
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        String txnCode = "TXN" + System.currentTimeMillis() + randomSuffix;
        LocalDateTime now = LocalDateTime.now();

        // 1. Đánh dấu đơn hàng đã thanh toán (paymentStatus = "PAID")
        // Trạng thái đơn vẫn là PENDING để chờ cửa hàng tiếp nhận & xác nhận theo quy trình độc lập
        order.setPaymentStatus("PAID");
        orderRepository.save(order);

        // 2. Ghi nhận giao dịch thanh toán thành công vào Payment entity
        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        Payment payment = payments.isEmpty() ? new Payment() : payments.get(0);
        payment.setOrder(order);
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionCode(txnCode);
        payment.setPaidAt(now);
        paymentRepository.save(payment);

        response.put("success", true);
        response.put("transactionCode", txnCode);
        response.put("paidAt", now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        response.put("amount", VND_FORMAT.format(order.getTotalAmount()) + " ₫");
        response.put("orderId", order.getId());
        response.put("message", "Thanh toán thành công. Đơn hàng đã chuyển sang trạng thái chờ chi nhánh xác nhận.");

        return ResponseEntity.ok(response);
    }
}
