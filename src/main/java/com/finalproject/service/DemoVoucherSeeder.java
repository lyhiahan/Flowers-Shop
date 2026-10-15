package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoVoucherSeeder {
    private final VoucherRepository voucherRepository;

    @Transactional
    public void seed(Store storeB) {
        if (!voucherRepository.existsByCode("Q7PHUMYHUNG")) {
            voucherRepository.save(Voucher.builder()
                    .code("Q7PHUMYHUNG")
                    .name("Ưu Đãi 15% Hoa Tươi Khai Trương Phú Mỹ Hưng")
                    .discountType("PERCENT")
                    .discountValue(new BigDecimal("15"))
                    .minOrderValue(new BigDecimal("300000"))
                    .maxDiscountAmount(new BigDecimal("150000"))
                    .store(storeB)
                    .quantity(150)
                    .usedCount(24)
                    .startDate(LocalDate.now().minusDays(15))
                    .endDate(LocalDate.now().plusMonths(3))
                    .isActive(true)
                    .createdAt(LocalDateTime.now().minusDays(15))
                    .build());
        }

        if (!voucherRepository.existsByCode("Q7FREESHIP")) {
            voucherRepository.save(Voucher.builder()
                    .code("Q7FREESHIP")
                    .name("Freeship Đơn Hoa Tươi Nội Thành Quận 7")
                    .discountType("FREE_SHIP")
                    .discountValue(new BigDecimal("35000"))
                    .minOrderValue(new BigDecimal("250000"))
                    .store(storeB)
                    .quantity(100)
                    .usedCount(42)
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now().plusMonths(2))
                    .isActive(true)
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .build());
        }

        if (!voucherRepository.existsByCode("Q7TRIAN50K")) {
            voucherRepository.save(Voucher.builder()
                    .code("Q7TRIAN50K")
                    .name("Tri Ân Khách Hàng Thân Thiết Q7 - Giảm 50K")
                    .discountType("FIXED_AMOUNT")
                    .discountValue(new BigDecimal("50000"))
                    .minOrderValue(new BigDecimal("500000"))
                    .store(storeB)
                    .quantity(80)
                    .usedCount(15)
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now().plusMonths(2))
                    .isActive(true)
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .build());
        }

        if (!voucherRepository.existsByCode("Q7VIP100K")) {
            voucherRepository.save(Voucher.builder()
                    .code("Q7VIP100K")
                    .name("Đặc Quyền Khách Hàng VIP Quận 7 - Giảm 100K")
                    .discountType("FIXED_AMOUNT")
                    .discountValue(new BigDecimal("100000"))
                    .minOrderValue(new BigDecimal("1000000"))
                    .store(storeB)
                    .quantity(50)
                    .usedCount(8)
                    .startDate(LocalDate.now().minusDays(2))
                    .endDate(LocalDate.now().plusMonths(3))
                    .isActive(true)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build());
        }
    }

}
