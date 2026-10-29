package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

@Service @RequiredArgsConstructor
public class OrderAftercareService {
    private final OrderAftercareRepository records;
    private final OrderWorkflowService workflow;
    private final StoreAccessService access;
    private final CloudinaryService images;
    private final NotificationService notifications;
    private final UserRepository users;
    private final jakarta.persistence.EntityManager em;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void addToModel(Order order, Model model) {
        User actor = access.currentUser();
        boolean owner = actor.isCustomerAccount() && actor.getId().equals(order.getUser().getId());
        if (!owner) access.requireStore(order.getPickupStore());
        boolean manager = actor.hasRole(Role.ADMIN) || actor.hasRole(Role.MANAGER);
        OrderAftercare care = records.findById(order.getId()).orElseGet(() -> OrderAftercare.builder().order(order).orderId(order.getId()).build());
        model.addAttribute("careEnabled", true);
        model.addAttribute("care", care);
        model.addAttribute("careOwner", owner);
        model.addAttribute("careWorker", !owner);
        model.addAttribute("careManager", !owner && manager);
        model.addAttribute("careAfterDelivery", delivered(order));
    }

    public String detailPath(Long id) {
        User actor = access.currentUser();
        if (actor.hasRole(Role.ADMIN)) return "/admin/orders/" + id;
        if (actor.hasRole(Role.MANAGER)) return "/manager/orders/" + id;
        if (actor.hasRole(Role.STAFF)) return "/staff/orders/" + id;
        return "/customer/orders/detail/" + id;
    }

    @Transactional(rollbackFor = IOException.class)
    public void sendFlowerPhoto(Long id, MultipartFile photo, String note) throws IOException {
        Order order = lock(id); access.requireStore(order.getPickupStore());
        if (order.getStatus() != OrderStatus.PREPARING) throw new IllegalStateException("Chỉ gửi ảnh thành phẩm khi đơn đang chuẩn bị hoa.");
        String clean = text(note, 0, 500, "Ghi chú mẫu hoa");
        var upload = requiredImage(photo, "flower_shop_order_proofs");
        OrderAftercare care = record(order);
        images.delete(care.getFlowerPhotoPublicId());
        care.setFlowerPhotoUrl(upload.secureUrl()); care.setFlowerPhotoPublicId(upload.publicId());
        care.setPhotoRevision(care.getPhotoRevision() + 1); care.setPhotoStatus("WAITING");
        care.setFloristNote(clean); care.setPhotoUploadedAt(now()); care.setPhotoUploadedBy(access.currentUser().getUsername());
        care.setApprovalComment(null); care.setPhotoReviewedAt(null); records.save(care);
        customer(order, "Ảnh thành phẩm cần duyệt", "Chi nhánh đã gửi ảnh mẫu hoa. Vui lòng xem và xác nhận trước khi giao.");
    }

    @Transactional
    public void reviewFlowerPhoto(Long id, int revision, String decision, String comment) {
        Order order = lock(id); requireOwner(order);
        OrderAftercare care = record(order);
        if (order.getStatus() != OrderStatus.PREPARING || !"WAITING".equals(care.getPhotoStatus()))
            throw new IllegalStateException("Ảnh không còn chờ duyệt. Vui lòng tải lại đơn hàng.");
        if (care.getPhotoRevision() != revision) throw new IllegalStateException("Ảnh đã được cập nhật. Vui lòng xem ảnh mới trước khi duyệt.");
        if (decision == null || !Set.of("APPROVED", "CHANGES_REQUESTED").contains(decision)) throw new IllegalArgumentException("Lựa chọn duyệt ảnh không hợp lệ.");
        care.setApprovalComment(text(comment, "CHANGES_REQUESTED".equals(decision) ? 5 : 0, 500, "Ý kiến về ảnh"));
        care.setPhotoStatus(decision); care.setPhotoReviewedAt(now()); records.save(care);
        branch(order, "Khách phản hồi ảnh thành phẩm", care.getPhotoStatusLabel());
    }

    @Transactional(rollbackFor = IOException.class)
    public void recordDelivery(Long id, MultipartFile photo, String note) throws IOException {
        Order order = lock(id); access.requireStore(order.getPickupStore());
        if (order.getStatus() != OrderStatus.DELIVERING && !delivered(order))
            throw new IllegalStateException("Chỉ ghi nhận bàn giao khi đang giao hoặc đã giao hoa.");
        OrderAftercare care = record(order);
        if (care.getDeliveryRecordedAt() != null) throw new IllegalStateException("Đã có minh chứng bàn giao; không được ghi đè lịch sử.");
        String clean = text(note, 5, 500, "Nội dung bàn giao");
        var upload = requiredImage(photo, "flower_shop_delivery_proofs");
        care.setDeliveryPhotoUrl(upload.secureUrl()); care.setDeliveryPhotoPublicId(upload.publicId());
        care.setDeliveryNote(clean); care.setDeliveryRecordedAt(now()); care.setDeliveryRecordedBy(access.currentUser().getUsername());
        records.save(care); customer(order, "Minh chứng bàn giao hoa", "Chi nhánh đã bổ sung ảnh và ghi chú bàn giao cho đơn của bạn.");
    }

    @Transactional
    public void confirmReceived(Long id) {
        Order order = lock(id); requireOwner(order);
        if (!delivered(order)) throw new IllegalStateException("Chỉ xác nhận nhận hoa khi đơn đã được ghi nhận giao thành công.");
        OrderAftercare care = record(order);
        if (care.getCustomerReceivedAt() != null) return;
        care.setCustomerReceivedAt(now()); records.save(care);
        branch(order, "Khách xác nhận đã nhận hoa", "Khách đã xác nhận nhận hoa cho đơn #" + id + ".");
    }

    @Transactional(rollbackFor = IOException.class)
    public void openSupport(Long id, String reason, String description, MultipartFile photo) throws IOException {
        Order order = lock(id); requireOwner(order);
        if (!delivered(order)) throw new IllegalStateException("Yêu cầu sau giao chỉ dành cho đơn đã giao hoặc hoàn tất.");
        if (reason == null || !Set.of("DAMAGED", "WRONG_ITEM", "LATE", "OTHER").contains(reason)) throw new IllegalArgumentException("Lý do hỗ trợ không hợp lệ.");
        OrderAftercare care = record(order);
        if (!"NONE".equals(care.getComplaintStatus())) throw new IllegalStateException("Đơn đã có yêu cầu hỗ trợ. Vui lòng xem phản hồi của cửa hàng.");
        String clean = text(description, 10, 1000, "Nội dung hỗ trợ");
        if (photo != null && !photo.isEmpty()) {
            var upload = requiredImage(photo, "flower_shop_support_proofs");
            care.setComplaintPhotoUrl(upload.secureUrl()); care.setComplaintPhotoPublicId(upload.publicId());
        }
        care.setComplaintReason(reason); care.setComplaintDescription(clean); care.setComplaintStatus("OPEN"); care.setComplaintCreatedAt(now());
        records.save(care); branch(order, "Yêu cầu hỗ trợ sau giao", "Khách đã gửi yêu cầu hỗ trợ cho đơn #" + id + ".");
        for (User admin : users.findByRole(Role.ADMIN)) notifications.sendNotification(admin, "Yêu cầu hỗ trợ sau giao", "Đơn #" + id + " có yêu cầu hỗ trợ mới.", "ORDER_SUPPORT", id);
    }

    @Transactional
    public void respondSupport(Long id, String status, String response) {
        Order order = lock(id); access.requireManager(order.getPickupStore()); access.requireStore(order.getPickupStore());
        OrderAftercare care = record(order);
        if (!Set.of("OPEN", "IN_REVIEW").contains(care.getComplaintStatus())) throw new IllegalStateException("Yêu cầu không còn chờ xử lý.");
        if (status == null || !Set.of("IN_REVIEW", "RESOLVED", "REJECTED").contains(status)) throw new IllegalArgumentException("Trạng thái hỗ trợ không hợp lệ.");
        care.setSupportResponse(text(response, 10, 1000, "Phản hồi cửa hàng")); care.setComplaintStatus(status);
        care.setSupportUpdatedAt(now()); care.setSupportUpdatedBy(access.currentUser().getUsername()); records.save(care);
        customer(order, "Cửa hàng phản hồi hỗ trợ", "Yêu cầu hỗ trợ đơn #" + id + " đã được cập nhật. Vui lòng xem chi tiết.");
    }

    private Order lock(Long id) {
        Order order = workflow.lockedOrder(id);
        em.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return order;
    }
    private OrderAftercare record(Order order) {
        return records.findById(order.getId()).orElseGet(() -> OrderAftercare.builder().order(order).orderId(order.getId()).build());
    }
    private void requireOwner(Order order) {
        User actor = access.currentUser();
        if (!actor.isCustomerAccount() || !actor.getId().equals(order.getUser().getId())) throw new AccessDeniedException("Bạn không có quyền xử lý đơn hàng này.");
    }
    private boolean delivered(Order order) { return order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED; }
    private CloudinaryService.UploadResult requiredImage(MultipartFile photo, String folder) throws IOException {
        if (photo == null || photo.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ảnh minh chứng.");
        return images.upload(photo, folder);
    }
    private String text(String value, int min, int max, String label) {
        String clean = value == null ? "" : value.strip();
        if (clean.length() < min || clean.length() > max) throw new IllegalArgumentException(label + " phải có từ " + min + " đến " + max + " ký tự.");
        return clean;
    }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private void customer(Order order, String title, String content) { notifications.sendNotification(order.getUser(), title, content, "ORDER_SUPPORT", order.getId()); }
    private void branch(Order order, String title, String content) { notifications.sendNotificationToStoreManagers(order.getPickupStore().getId(), title, content, "ORDER_SUPPORT", order.getId()); }
}
