package com.finalproject.controller.customer;

import com.finalproject.service.CatalogStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** Public quantities only: no warehouse/private products or customer/order data. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/catalog-stock")
public class CatalogStockApiController {
    private final CatalogStockService stock;
    public record Snapshot(long currentStockQuantity, Map<Long, CatalogStockService.Stock> products) {}

    @GetMapping
    public ResponseEntity<Snapshot> snapshot(
            @RequestParam(value = "productIds", required = false) List<Long> productIds,
            @RequestParam(value = "storeId", required = false) Long storeId) {
        List<Long> ids = productIds == null ? List.of() : productIds;
        if (ids.size() > 32 || ids.stream().anyMatch(id -> id == null || id <= 0)
                || storeId != null && storeId <= 0)
            throw new IllegalArgumentException("Tham số tồn kho không hợp lệ; tối đa 32 sản phẩm mỗi lần.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Snapshot(stock.availableQuantity(storeId), stock.summarizeIds(ids, storeId)));
    }
}
