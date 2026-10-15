package com.finalproject.utils;

import java.math.BigDecimal;

/** Product prices are amounts in whole Vietnamese dong, not discount percentages. */
public final class ProductPrices {
    private ProductPrices() {}

    public static boolean valid(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ONE) >= 0
                && value.stripTrailingZeros().scale() <= 0
                && value.precision() - value.scale() <= 16;
    }

    public static void validate(BigDecimal price, BigDecimal promotion) {
        if (!valid(price) || (promotion != null && (!valid(promotion) || promotion.compareTo(price) > 0))) {
            throw new IllegalArgumentException("Giá bán và giá khuyến mãi phải là số đồng nguyên, từ 1đ, tối đa 16 chữ số; giá khuyến mãi không vượt quá giá bán. Để trống nếu không khuyến mãi.");
        }
    }
}
