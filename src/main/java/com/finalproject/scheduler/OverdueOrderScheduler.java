package com.finalproject.scheduler;

import com.finalproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.rental.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OverdueOrderScheduler {

    private final OrderService orderService;

    // Chạy mỗi ngày lúc 07:00 sáng
    @Scheduled(cron = "0 0 7 * * ?")
    public void scheduleOverdueOrdersCheck() {
        log.info("Bắt đầu quét định kỳ các đơn hàng thuê quá hạn...");
        orderService.scanAndMarkOverdueOrders();
        log.info("Hoàn tất quét đơn hàng thuê quá hạn.");
    }
}
