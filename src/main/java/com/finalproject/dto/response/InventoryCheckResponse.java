package com.finalproject.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckResponse {
    private boolean available;
    private int currentStock;
    private String alternativeSuggestion; // Gợi ý: "Chi nhánh B cách 2.3km còn 5 sản phẩm"
    private Long alternativeStoreId;
}