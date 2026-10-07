package com.finalproject.config;

import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.OrderRepository;
import com.finalproject.service.OrderWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Component @Profile("!test") @Order(30) @RequiredArgsConstructor @Slf4j
public class OrderExpirySchedule implements ApplicationRunner {
    private final OrderRepository orders;
    private final OrderWorkflowService workflow;
    private final Clock clock;
    @Override @Transactional
    public void run(ApplicationArguments args) {
        // Legacy orders receive a full grace period from rollout, rather than expiring immediately.
        LocalDateTime now = LocalDateTime.now(clock);
        orders.initializeLegacyPaymentDeadlines(now.plusMinutes(30), now.plusHours(24));
    }
    @Scheduled(fixedDelay=60000, initialDelay=60000)
    public void expire() {
        LocalDateTime now = LocalDateTime.now(clock);
        var due = orders.findAll((root,query,cb) -> cb.and(cb.lessThanOrEqualTo(root.get("paymentExpiresAt"),now),
                cb.equal(root.get("paymentStatus"),"UNPAID"),root.get("status").in(OrderStatus.PENDING,OrderStatus.CONFIRMED)),
                org.springframework.data.domain.PageRequest.of(0,100,org.springframework.data.domain.Sort.by("id")));
        for (var order : due) try { workflow.expireUnpaid(order.getId(),now); }
        catch (RuntimeException ex) { log.warn("Chưa xử lý được đơn hết hạn #{}",order.getId(),ex); }
    }
}
