package com.finalproject.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartSeriesDto {
    private String timeframe; // hour, day, month, year
    private List<String> labels;
    private List<BigDecimal> data;
    private BigDecimal total;
}
