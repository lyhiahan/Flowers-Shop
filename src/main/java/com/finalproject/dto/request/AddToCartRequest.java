package com.finalproject.dto.request;

import com.finalproject.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {

    private Long productId;
    private Long storeId;
    private OrderType orderType; // BUY hoặc RENT
    private Integer quantity = 1;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate rentStartDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate rentEndDate;
}