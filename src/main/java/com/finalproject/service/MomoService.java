package com.finalproject.service;

import com.finalproject.entity.Order;
import jakarta.servlet.http.HttpServletRequest;

public interface MomoService {
    /**
     * Tạo đường dẫn thanh toán MoMo cho đơn hàng (hỗ trợ cả MoMo Gateway Sandbox lẫn mô phỏng fallback)
     */
    String createPaymentUrl(Order order, HttpServletRequest request);

    /**
     * Xử lý xác nhận kết quả thanh toán từ MoMo
     */
    boolean processPaymentResult(Long orderId, int resultCode, String transId);
}
