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
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public Voucher findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        Voucher voucher = voucherRepository.findByCodeAndIsActiveTrue(code.trim().toUpperCase(java.util.Locale.ROOT)).orElse(null);
        if (voucher != null) entityManager.refresh(voucher, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return voucher;
    }

    @Override
    public List<Voucher> getAvailableVouchers(Long storeId) {
        return voucherRepository.findAvailableVouchers(storeId, LocalDate.now());
    }

    @Override
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount, BigDecimal shippingFee) {
        return calculateDiscount(voucher, orderAmount, shippingFee, null);
    }

    @Override
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount, BigDecimal shippingFee, BigDecimal minAllowedAmount) {
        if (voucher == null) return BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive()) || voucher.getStartDate() == null || voucher.getEndDate() == null
                || today.isBefore(voucher.getStartDate()) || today.isAfter(voucher.getEndDate())
                || voucher.getUsedCount() >= voucher.getQuantity()) {
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn hoặc hết lượt sử dụng.");
        }
        if (voucher.getDiscountValue() == null || voucher.getDiscountValue().signum() < 0) {
            throw new IllegalArgumentException("Giá trị giảm giá không hợp lệ.");
        }
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
            if (voucher.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("Tỷ lệ giảm không được vượt quá 100%.");
            BigDecimal percent = voucher.getDiscountValue().movePointLeft(2);
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

        // Ràng buộc bảo toàn: Đảm bảo khi áp mã giảm giá thì đơn hàng không bị giảm thấp hơn giá nhập gốc
        if (!"FREE_SHIP".equalsIgnoreCase(voucher.getDiscountType())
                && minAllowedAmount != null && minAllowedAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxAllowedDiscount = orderAmount.subtract(minAllowedAmount).max(BigDecimal.ZERO);
            if (discount.compareTo(maxAllowedDiscount) > 0) {
                discount = maxAllowedDiscount;
            }
        }

        return discount.max(BigDecimal.ZERO).min(orderAmount.add(shippingFee != null ? shippingFee : BigDecimal.ZERO))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public Voucher createVoucher(Voucher voucher) {
        if (voucher.getCode() == null || voucher.getCode().isBlank() || voucher.getName() == null || voucher.getName().isBlank()
                || voucher.getStartDate() == null || voucher.getEndDate() == null || voucher.getEndDate().isBefore(voucher.getStartDate())
                || voucher.getQuantity() == null || voucher.getQuantity() < 1 || voucher.getUsedCount() == null || voucher.getUsedCount() < 0
                || voucher.getDiscountValue() == null || voucher.getDiscountValue().signum() < 0
                || (voucher.getMinOrderValue() != null && voucher.getMinOrderValue().signum() < 0)
                || (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().signum() < 0)
                || !java.util.Set.of("PERCENT", "FIXED_AMOUNT", "FREE_SHIP").contains(voucher.getDiscountType())
                || ("PERCENT".equals(voucher.getDiscountType()) && voucher.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Thông tin mã giảm giá không hợp lệ.");
        }
        if (voucher.getCode() != null) {
            voucher.setCode(voucher.getCode().trim().toUpperCase());
        }
        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id).orElseThrow();
        voucher.setIsActive(false);
        voucherRepository.save(voucher);
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
