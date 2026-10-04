package com.finalproject.service;

import com.finalproject.entity.Order;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface MomoService {
    String createPaymentUrl(Order order, HttpServletRequest request);
    boolean processNotification(Map<String, Object> payload);
    void simulatePayment(Long orderId, String username);
}
