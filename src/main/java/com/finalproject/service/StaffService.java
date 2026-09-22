package com.finalproject.service;

import com.finalproject.entity.Order;
import java.math.BigDecimal;
import java.util.List;

public interface StaffService {
    List<Order> getOrdersByStaffStore(String staffUsername);
    void approveOrder(String staffUsername, Long orderId);
    void deliverOrder(String staffUsername, Long orderId);
    void processReturn(String staffUsername, Long orderId, Long returnStoreId, boolean waiveFine, BigDecimal damageFee);
    void convertRentToBuy(String staffUsername, Long orderId);
}