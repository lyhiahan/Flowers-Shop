package com.finalproject.entity;

import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType orderType;

    // Chi nhánh xuất đồ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_store_id", nullable = false)
    private Store pickupStore;

    // Chi nhánh nhận trả đồ (hỗ trợ trả chéo kho)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_store_id")
    // Legacy database field: retained for historical records; never used by flower checkout.
    private Store returnStore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    private BigDecimal totalAmount;
    // Legacy database field: retained for historical records; never used by flower checkout.
    private BigDecimal totalDeposit;
    // Legacy database field: retained for historical records; never used by flower checkout.
    private BigDecimal fineAmount;

    // Snapshot: subsequent delivery tariff changes must not change existing orders.
    @Column(name = "shipping_fee", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "delivery_unit_id")
    private Long deliveryUnitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id")
    // Historical delivery contact only; no assignment or account role.
    private User legacyDeliveryContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "discount_amount")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private String paymentMethod = "COD"; // COD, VNPAY, MOMO

    @Column(name = "payment_status", length = 20)
    @Builder.Default
    private String paymentStatus = "UNPAID"; // UNPAID, PAID

    @Enumerated(EnumType.STRING)
    @Column(name = "funds_status", nullable = false, length = 20)
    @Builder.Default
    private com.finalproject.enums.OrderFundsStatus fundsStatus = com.finalproject.enums.OrderFundsStatus.NONE;

    @Column(name = "funds_amount", precision = 18, scale = 2)
    private BigDecimal fundsAmount;

    @Column(name = "commission_rate_snapshot", precision = 9, scale = 4)
    private BigDecimal commissionRateSnapshot;

    @Column(name="payment_expires_at")
    private LocalDateTime paymentExpiresAt;
    @Builder.Default @Column(name="payment_expired")
    private Boolean paymentExpired = false;

    @org.hibernate.annotations.Nationalized
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "shipping_method", length = 20)
    @Builder.Default
    private String shippingMethod = "DELIVERY";

    // Thông tin giao hoa & thiệp chúc mừng
    private java.time.LocalDate deliveryDate;
    private java.time.LocalTime deliveryTime;
    @org.hibernate.annotations.Nationalized
    @Column(length = 500)
    private String greetingMessage; // Lời chúc trên thiệp/banner
    @org.hibernate.annotations.Nationalized
    @Column(length = 100)
    private String recipientName; // Tên người nhận hoa
    @Column(length = 20)
    private String recipientPhone; // SĐT người nhận hoa
    @org.hibernate.annotations.Nationalized
    @Column(length = 255)
    private String recipientAddress; // Địa chỉ giao hoa

    private LocalDateTime createdAt;

    @Version
    private Long version; // Khóa lạc quan chống duyệt trùng

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;
    // Customers may cancel only before the florist starts preparing; creation time is irrelevant.
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    @Transient
    public boolean isPickup() {
        return "PICKUP".equals(shippingMethod) || "STORE_PICKUP".equals(shippingMethod);
    }

    @Transient
    public BigDecimal getAmountToCollect() {
        if ((!"COD".equals(paymentMethod) && !"CASH".equals(paymentMethod)) || "PAID".equals(paymentStatus) || "REFUNDED".equals(paymentStatus)
                || status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED || status == OrderStatus.REFUND_REQUESTED)
            return BigDecimal.ZERO;
        return totalAmount == null ? BigDecimal.ZERO : totalAmount.max(BigDecimal.ZERO);
    }

    @Transient
    public String getCancellationActorDisplay() {
        if (cancellationReason == null || cancellationReason.isBlank()) return null;
        String lower = cancellationReason.toLowerCase();
        if (lower.contains("admin")) return "Đã hủy bởi Admin";
        if (lower.contains("quản lý") || lower.contains("cửa hàng")) return "Đã hủy bởi Cửa hàng";
        return "Đã hủy đơn";
    }

    @Transient
    public String getCancellationReasonDetail() {
        if (cancellationReason == null || cancellationReason.isBlank()) return null;
        String text = cancellationReason.trim();
        text = text.replace(" Đơn chưa thanh toán đã được hủy.", "").replace("Đơn chưa thanh toán đã được hủy.", "").trim();
        int idx = text.indexOf("với lý do:");
        if (idx >= 0) {
            text = text.substring(idx + "với lý do:".length()).trim();
        }
        if (text.toLowerCase().startsWith("lý do:")) {
            text = text.substring("lý do:".length()).trim();
        }
        return text.replace("ví demo", "ví");
    }
}

