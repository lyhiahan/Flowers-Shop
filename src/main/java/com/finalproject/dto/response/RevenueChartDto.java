package com.finalproject.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueChartDto {
    private BigDecimal totalBuyRevenue;   // Tổng doanh thu Mua
}
