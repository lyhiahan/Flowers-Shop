package com.finalproject.service.impl;

import com.finalproject.entity.Voucher;
import com.finalproject.repository.VoucherRepository;
import com.finalproject.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public Voucher findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        return voucherRepository.findByCodeAndIsActiveTrue(code.trim().toUpperCase())
                .orElse(null);
    }

    @Override
    public List<Voucher> getAvailableVouchers(Long storeId) {
        return voucherRepository.findAvailableVouchers(storeId, LocalDate.now());
    }

    @Override
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount, BigDecimal shippingFee) {
        if (voucher == null) return BigDecimal.ZERO;
        if (voucher.getMinOrderValue() != null && orderAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getMinOrderValue() + "đ để áp dụng mã!");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if ("FREE_SHIP".equalsIgnoreCase(voucher.getDiscountType())) {
            // Giảm tối đa toàn bộ tiền ship
            discount = shippingFee != null ? shippingFee : BigDecimal.ZERO;
            if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            // Giảm theo % giá trị đơn
            BigDecimal percent = voucher.getDiscountValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            discount = orderAmount.multiply(percent);
            if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(voucher.getDiscountType())) {
            // Giảm số tiền cố định
            discount = voucher.getDiscountValue();
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
        }

        return discount;
    }

    @Override
    @Transactional
    public Voucher createVoucher(Voucher voucher) {
        if (voucher.getCode() != null) {
            voucher.setCode(voucher.getCode().trim().toUpperCase());
        }
        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        voucherRepository.deleteById(id);
    }

    @Override
    public List<Voucher> getVouchersByStore(Long storeId) {
        return voucherRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    @Override
    public List<Voucher> getSystemVouchers() {
        return voucherRepository.findByStoreIsNullOrderByCreatedAtDesc();
    }
}
