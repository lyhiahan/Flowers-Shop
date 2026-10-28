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
    // Historical database values; the sales workflow cannot enter these states.
    RENTING,
    RETURNED,
    OVERDUE;

    public boolean isLegacy() {
        return this == RENTING || this == RETURNED || this == OVERDUE;
    }

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
            case RENTING -> "Giao dịch lịch sử đang mở";
            case RETURNED -> "Giao dịch lịch sử đã đóng";
            case OVERDUE -> "Giao dịch lịch sử cần đối chiếu";
        };
    }
}
