package com.finalproject.enums;

public enum OrderStatus {
    PENDING,    // Chờ duyệt
    CONFIRMED,  // Đã duyệt đơn
    RENTING,    // Đang thuê đồ
    RETURNED,   // Đã trả đồ
    OVERDUE,    // Quá hạn trả
    COMPLETED,  // Hoàn tất đơn mua hoặc tất toán thuê
    CANCELLED   // Đã hủy đơn
}