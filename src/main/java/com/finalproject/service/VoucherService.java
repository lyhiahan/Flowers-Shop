package com.finalproject.service;

import com.finalproject.entity.Voucher;
import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    Voucher findByCode(String code);
    List<Voucher> getAvailableVouchers(Long storeId);
    BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount, BigDecimal shippingFee);
    Voucher createVoucher(Voucher voucher);
    void deleteVoucher(Long id);
    List<Voucher> getVouchersByStore(Long storeId);
    List<Voucher> getSystemVouchers();
}
