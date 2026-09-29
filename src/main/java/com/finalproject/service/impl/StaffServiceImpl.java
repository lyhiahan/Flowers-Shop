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

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {
    private final OrderRepository orders;
    private final StoreAccessService access;
    private final OrderWorkflowService workflow;

    @Override
    public List<Order> getOrdersByStaffStore(String username) {
        return orders.findByPickupStoreIdOrderByCreatedAtDesc(access.assignedStore().getId());
    }
    @Override
    public void approveOrder(String username, Long id) { workflow.transition(id, OrderStatus.CONFIRMED); }
    @Override
    public void deliverOrder(String username, Long id) { workflow.transition(id, OrderStatus.COMPLETED); }
    @Override
    public void processReturn(String username, Long id, Long store, boolean waive, BigDecimal damage) {
        throw new IllegalStateException("Hệ thống bán hoa không hỗ trợ trả đồ thuê.");
    }
    @Override
    public void convertRentToBuy(String username, Long id) {
        throw new IllegalStateException("Hệ thống bán hoa không hỗ trợ chuyển đơn thuê.");
    }
}