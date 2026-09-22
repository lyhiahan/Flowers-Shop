package com.finalproject.controller;

import com.finalproject.dto.response.InventoryCheckResponse;
import com.finalproject.entity.Store;
import com.finalproject.entity.StoreInventory;
import com.finalproject.repository.StoreInventoryRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.utils.DistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryApiController {

    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreRepository storeRepository;

    @GetMapping("/check")
    public InventoryCheckResponse checkStock(@RequestParam("productId") Long productId,
                                             @RequestParam("storeId") Long storeId) {
        Store currentStore = storeRepository.findById(storeId).orElse(null);
        StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId).orElse(null);

        int stock = (inventory != null) ? inventory.getQuantity() : 0;

        if (stock > 0) {
            return InventoryCheckResponse.builder()
                    .available(true)
                    .currentStock(stock)
                    .build();
        }

        // Nếu hết hàng (stock = 0), tìm chi nhánh khác còn hàng gần nhất
        List<StoreInventory> alternatives = storeInventoryRepository.findAlternativeStoresWithStock(productId, storeId);
        if (alternatives.isEmpty() || currentStore == null) {
            return InventoryCheckResponse.builder()
                    .available(false)
                    .currentStock(0)
                    .alternativeSuggestion("Sản phẩm đã hết hàng trên toàn bộ hệ thống chi nhánh.")
                    .build();
        }

        // Tìm chi nhánh gần nhất dựa vào công thức tính khoảng cách
        StoreInventory nearestInv = null;
        double minDistance = Double.MAX_VALUE;

        for (StoreInventory inv : alternatives) {
            Store altStore = inv.getStore();
            if (altStore.getLatitude() != null && altStore.getLongitude() != null &&
                currentStore.getLatitude() != null && currentStore.getLongitude() != null) {

                double dist = DistanceUtil.calculateDistance(
                        currentStore.getLatitude(), currentStore.getLongitude(),
                        altStore.getLatitude(), altStore.getLongitude());

                if (dist < minDistance) {
                    minDistance = dist;
                    nearestInv = inv;
                }
            }
        }

        if (nearestInv != null) {
            String suggestion = "Chi nhánh " + nearestInv.getStore().getName() + " (cách " + minDistance + "km) vẫn còn " +
                                nearestInv.getQuantity() + " sản phẩm. Bạn có muốn đổi sang chi nhánh này?";
            return InventoryCheckResponse.builder()
                    .available(false)
                    .currentStock(0)
                    .alternativeSuggestion(suggestion)
                    .alternativeStoreId(nearestInv.getStore().getId())
                    .build();
        }

        return InventoryCheckResponse.builder()
                .available(false)
                .currentStock(0)
                .alternativeSuggestion("Chi nhánh hiện tại đã hết hàng.")
                .build();
    }
}