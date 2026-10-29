package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Evidence and support records are separate from payment and order accounting. */
@Entity @Table(name = "order_aftercare")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderAftercare {
    @Id @Column(name = "order_id") private Long orderId;
    @MapsId @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") private Order order;
    @Column(length = 500) private String flowerPhotoUrl;
    @Column(length = 255) private String flowerPhotoPublicId;
    @Builder.Default @Column(nullable = false) private int photoRevision = 0;
    @Builder.Default @Column(nullable = false, length = 30) private String photoStatus = "NONE";
    @org.hibernate.annotations.Nationalized @Column(length = 500) private String floristNote;
    private LocalDateTime photoUploadedAt;
    @Column(length = 100) private String photoUploadedBy;
    @org.hibernate.annotations.Nationalized @Column(length = 500) private String approvalComment;
    private LocalDateTime photoReviewedAt;
    @Column(length = 500) private String deliveryPhotoUrl;
    @Column(length = 255) private String deliveryPhotoPublicId;
    @org.hibernate.annotations.Nationalized @Column(length = 500) private String deliveryNote;
    private LocalDateTime deliveryRecordedAt;
    @Column(length = 100) private String deliveryRecordedBy;
    private LocalDateTime customerReceivedAt;
    @Builder.Default @Column(nullable = false, length = 30) private String complaintStatus = "NONE";
    @Column(length = 30) private String complaintReason;
    @org.hibernate.annotations.Nationalized @Column(length = 1000) private String complaintDescription;
    @Column(length = 500) private String complaintPhotoUrl;
    @Column(length = 255) private String complaintPhotoPublicId;
    private LocalDateTime complaintCreatedAt;
    @org.hibernate.annotations.Nationalized @Column(length = 1000) private String supportResponse;
    private LocalDateTime supportUpdatedAt;
    @Column(length = 100) private String supportUpdatedBy;
    @Version private Long version;

    public boolean isAwaitingPhotoApproval() { return "WAITING".equals(photoStatus) || "CHANGES_REQUESTED".equals(photoStatus); }
    public String getPhotoStatusLabel() {
        return switch (photoStatus) { case "WAITING" -> "Chờ khách duyệt ảnh"; case "APPROVED" -> "Khách đã đồng ý";
            case "CHANGES_REQUESTED" -> "Khách yêu cầu chỉnh lại"; default -> "Chưa gửi ảnh thành phẩm"; };
    }
    public String getComplaintStatusLabel() {
        return switch (complaintStatus) { case "OPEN" -> "Đã gửi yêu cầu"; case "IN_REVIEW" -> "Đang xử lý";
            case "RESOLVED" -> "Đã giải quyết"; case "REJECTED" -> "Không chấp nhận yêu cầu"; default -> "Chưa có yêu cầu hỗ trợ"; };
    }
    public String getComplaintReasonLabel() {
        if (complaintReason == null) return "";
        return switch (complaintReason) { case "DAMAGED" -> "Hoa hư hỏng"; case "WRONG_ITEM" -> "Không đúng mẫu hoặc thiếu hoa";
            case "LATE" -> "Giao trễ"; default -> "Nội dung khác"; };
    }
}
