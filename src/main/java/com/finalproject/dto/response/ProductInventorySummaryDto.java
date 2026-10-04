package com.finalproject.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventorySummaryDto {
    private Long productId;
    private String productName;
    @Builder.Default
    private String unit = "bó";
    private int totalAvailable;
    private int totalPhysical;
    private int totalReserved;
    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    private String statusLabel; // Còn hàng, Sắp hết, Hết hàng

    @Builder.Default
    private List<BranchInventoryDto> branchStocks = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchInventoryDto {
        private Long storeId;
        private String storeName;
        private String storeAddress;
        private int physicalStock;
        private int reservedStock;
        private int availableStock;
        private String unit;
    }
}
