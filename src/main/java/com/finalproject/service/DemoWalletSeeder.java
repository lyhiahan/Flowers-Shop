package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoWalletSeeder {
    private final TransactionRepository transactionRepository;

    @Transactional
    public void seed(Store storeB, User vendorFlower, List<Order> paidOrders) {
        if (transactionRepository.findByStoreIdOrderByCreatedAtDesc(storeB.getId()).size() >= 5) {
            return;
        }

        BigDecimal commRate = new BigDecimal("0.08"); // 8% hoa hồng sàn

        for (Order order : paidOrders) {
            if (transactionRepository.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT").isEmpty()) {
                transactionRepository.save(Transaction.builder()
                        .store(storeB)
                        .user(order.getUser())
                        .order(order)
                        .isUp(true)
                        .amount(order.getTotalAmount())
                        .type("ORDER_PAYMENT")
                        .status("COMPLETED")
                        .description("Nhận thanh toán đơn hàng hoa tươi #" + order.getId() + " (" + order.getRecipientName() + ")")
                        .createdAt(order.getCreatedAt().plusMinutes(5))
                        .build());

                BigDecimal commFee = order.getTotalAmount().multiply(commRate).setScale(0, java.math.RoundingMode.HALF_UP);
                transactionRepository.save(Transaction.builder()
                        .store(storeB)
                        .user(vendorFlower)
                        .order(order)
                        .isUp(false)
                        .amount(commFee)
                        .type("COMMISSION_FEE")
                        .status("COMPLETED")
                        .description("Phí hoa hồng sàn 8% áp dụng cho đơn hàng #" + order.getId())
                        .createdAt(order.getCreatedAt().plusMinutes(6))
                        .build());
            }
        }

        transactionRepository.save(Transaction.builder()
                .store(storeB)
                .user(vendorFlower)
                .isUp(false)
                .amount(new BigDecimal("5000000"))
                .type("WITHDRAWAL")
                .status("COMPLETED")
                .description("Rút tiền doanh thu tuần về tài khoản Vietcombank - STK 0123456789 - Trần Đối Tác")
                .createdAt(LocalDateTime.now().minusDays(2).withHour(17).withMinute(0))
                .build());

        transactionRepository.save(Transaction.builder()
                .store(storeB)
                .user(vendorFlower)
                .isUp(false)
                .amount(new BigDecimal("10000000"))
                .type("WITHDRAWAL")
                .status("COMPLETED")
                .description("Rút tiền lợi nhuận bán hoa về tài khoản Techcombank - STK 1903456789 - Trần Đối Tác")
                .createdAt(LocalDateTime.now().minusDays(5).withHour(18).withMinute(30))
                .build());
    }

}
