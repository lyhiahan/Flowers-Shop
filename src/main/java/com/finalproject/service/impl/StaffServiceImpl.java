package com.finalproject.service.impl;

import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.OrderRepository;
import com.finalproject.service.StaffService;
import com.finalproject.service.StoreAccessService;
import com.finalproject.service.OrderWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {
    private final OrderRepository orders;
    private final StoreAccessService access;
    private final OrderWorkflowService workflow;
    private final jakarta.persistence.EntityManager entityManager;
    private static final Set<OrderStatus> STAFF_ACTIONS = Set.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING,
            OrderStatus.DELIVERING, OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private void requireActor(String username) {
        var actor = access.currentUser();
        if (!actor.getUsername().equals(username) || (!actor.isStoreWorker() && !actor.hasRole(com.finalproject.enums.Role.ADMIN)))
            throw new AccessDeniedException("Bạn không có quyền xử lý đơn tại quầy nhân viên.");
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> searchOrders(String username, String section, String keyword,
            OrderStatus status, String fulfillment, int page, int size) {
        requireActor(username);
        var store = access.assignedStore();
        String kw = keyword == null ? "" : keyword.strip();
        if (kw.length() > 100) throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        if (!Set.of("orders", "preparation", "delivery").contains(section)) throw new IllegalArgumentException("Trang công việc không hợp lệ.");
        if (!Set.of("ALL", "PICKUP", "DELIVERY").contains(fulfillment)) throw new IllegalArgumentException("Hình thức nhận hoa không hợp lệ.");
        return com.finalproject.utils.StorePages.load(page, size, pageable -> orders.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("pickupStore").get("id"), store.getId()));
            if ("preparation".equals(section)) predicates.add(root.get("status").in(OrderStatus.CONFIRMED, OrderStatus.PREPARING));
            if ("delivery".equals(section)) predicates.add(root.get("status").in(OrderStatus.PREPARING, OrderStatus.PICKED_UP, OrderStatus.DELIVERING, OrderStatus.DELIVERED));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            var pickup = root.get("shippingMethod").in("PICKUP", "STORE_PICKUP");
            if ("PICKUP".equals(fulfillment)) predicates.add(pickup);
            if ("DELIVERY".equals(fulfillment)) predicates.add(cb.not(pickup));
            if (!kw.isBlank()) {
                String escaped = "%" + kw.toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                var customer = root.join("user", jakarta.persistence.criteria.JoinType.LEFT);
                var matches = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                matches.add(cb.like(cb.lower(root.get("recipientName")), escaped, '!'));
                matches.add(cb.like(root.get("recipientPhone"), escaped, '!'));
                matches.add(cb.like(cb.lower(customer.get("fullName")), escaped, '!'));
                matches.add(cb.like(customer.get("phone"), escaped, '!'));
                try { matches.add(cb.equal(root.get("id"), Long.parseLong(kw.replaceFirst("(?i)^#?(HD)?", "")))); }
                catch (NumberFormatException ignored) { }
                predicates.add(cb.or(matches.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(String username, Long id) {
        requireActor(username);
        var order = orders.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
        access.requireStore(order.getPickupStore());
        return order;
    }

    @Override
    public List<OrderAction> actions(Order order) {
        return workflow.availableTransitions(order).stream().filter(STAFF_ACTIONS::contains)
                .filter(next -> (next != OrderStatus.DELIVERING && next != OrderStatus.DELIVERED && next != OrderStatus.COMPLETED)
                        || "COD".equals(order.getPaymentMethod()) || "CASH".equals(order.getPaymentMethod()) || "PAID".equals(order.getPaymentStatus()))
                .map(next -> {
            boolean collect = (next == OrderStatus.DELIVERED || next == OrderStatus.COMPLETED) && order.getAmountToCollect().signum() > 0;
            String label = switch (next) {
                case CONFIRMED -> "Xác nhận đơn";
                case PREPARING -> "Bắt đầu chuẩn bị hoa";
                case DELIVERING -> "Bàn giao vận chuyển · Đang giao";
                case DELIVERED -> "Xác nhận đã giao hoa";
                case COMPLETED -> order.isPickup() ? "Giao hoa tại quầy · Hoàn tất" : "Hoàn tất đơn hàng";
                default -> workflow.transitionLabel(order, next);
            };
            return new OrderAction(next, label, collect, collect ? order.getAmountToCollect() : BigDecimal.ZERO);
        }).toList();
    }

    @Override
    @Transactional
    public void updateStatus(String username, Long id, OrderStatus next, boolean codCollected) {
        requireActor(username);
        var order = orders.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        access.requireStore(order.getPickupStore());
        if (!STAFF_ACTIONS.contains(next)) throw new AccessDeniedException("Nhân viên không có quyền thực hiện thao tác này.");
        if ((next == OrderStatus.DELIVERED || next == OrderStatus.COMPLETED) && order.getAmountToCollect().signum() > 0 && !codCollected)
            throw new IllegalArgumentException("Vui lòng xác nhận đã giao hoa và thu đủ tiền COD trước khi hoàn tất.");
        workflow.transition(id, next);
    }

    @Override
    public List<Order> getOrdersByStaffStore(String username) {
        requireActor(username);
        return orders.findByPickupStoreIdOrderByCreatedAtDesc(access.assignedStore().getId());
    }
    @Override
    @Transactional
    public void approveOrder(String username, Long id) { updateStatus(username, id, OrderStatus.CONFIRMED, false); }
    @Override
    @Transactional
    public void deliverOrder(String username, Long id) { updateStatus(username, id, OrderStatus.COMPLETED, false); }
    @Override
    public void processReturn(String username, Long id, Long store, boolean waive, BigDecimal damage) {
        throw new IllegalStateException("Hệ thống bán hoa không hỗ trợ trả đồ thuê.");
    }
    @Override
    public void convertRentToBuy(String username, Long id) {
        throw new IllegalStateException("Hệ thống bán hoa không hỗ trợ chuyển đơn thuê.");
    }
}
