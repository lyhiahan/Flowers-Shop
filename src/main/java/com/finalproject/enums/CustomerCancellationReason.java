package com.finalproject.enums;

/** Các lý do khách hàng được chọn khi tự hủy đơn. */
public enum CustomerCancellationReason {
    CHANGE_ADDRESS("Muốn thay đổi địa chỉ nhận hàng"),
    CHANGE_VOUCHER("Muốn đổi mã giảm giá"),
    WRONG_PRODUCT("Đặt nhầm sản phẩm"),
    NO_LONGER_NEEDED("Thay đổi nhu cầu, không còn nhu cầu mua"),
    NOT_READY_TO_PAY("Chưa sẵn sàng thanh toán"),
    OTHER("Lý do khác");

    private final String label;

    CustomerCancellationReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static String resolve(String code, String otherReason) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn lý do hủy đơn.");
        }
        CustomerCancellationReason selected;
        try {
            selected = valueOf(code.strip());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Lý do hủy đơn không hợp lệ.");
        }
        if (selected != OTHER) return selected.label;
        String detail = otherReason == null ? "" : otherReason.strip().replaceAll("\\s+", " ");
        if (detail.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn khác.");
        if (detail.length() > 200) throw new IllegalArgumentException("Lý do khác không được vượt quá 200 ký tự.");
        return selected.label + ": " + detail;
    }
}
