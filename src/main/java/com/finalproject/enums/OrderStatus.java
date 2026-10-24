package com.finalproject.enums;

public enum OrderStatus {
    PENDING,    // Đơn hàng mới / Chờ tiếp nhận
    CONFIRMED,  // Đã xác nhận đơn
    PREPARING,  // Đang cắm hoa & chuẩn bị
    PICKED_UP,  // Trạng thái bàn giao vận chuyển của đơn cũ
    DELIVERING, // Đang giao hoa đến khách
    DELIVERED,  // Đã giao thành công
    COMPLETED,  // Giao hoa hoàn tất
    CANCELLED,  // Đã hủy đơn
    REFUND_REQUESTED,
    REFUNDED,   // Trả hàng - Hoàn tiền
    RENTING,    // Đang thuê phụ kiện/cổng hoa
    RETURNED,   // Đã hoàn trả phụ kiện
    OVERDUE;     // Quá hạn hoàn trả

    public String getDisplayName() {
        return switch (this) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case PREPARING -> "Đang chuẩn bị hoa";
            case PICKED_UP -> "Đã bàn giao vận chuyển";
            case DELIVERING -> "Đang giao hoa";
            case DELIVERED -> "Đã giao hoa";
            case COMPLETED -> "Hoàn tất";
            case CANCELLED -> "Đã hủy";
            case REFUND_REQUESTED -> "Chờ hoàn tiền";
            case REFUNDED -> "Đã hoàn tiền";
            case RENTING -> "Đang thuê";
            case RETURNED -> "Đã trả";
            case OVERDUE -> "Quá hạn";
        };
    }
}
