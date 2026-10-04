package com.finalproject.controller.customer;

import com.finalproject.entity.CartItem;
import com.finalproject.entity.StoreInventory;
import com.finalproject.repository.CartItemRepository;
import com.finalproject.repository.StoreInventoryRepository;
import com.finalproject.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

/**
 * API kiểm tra tồn kho chi nhánh khi khách chọn "Đến lấy tại cửa hàng".
 * Endpoint: GET /api/customer/pickup-check?storeId=X&itemIds=1,2,3
 *
 * Trả về JSON:
 * {
 *   "ok": true,          // chi nhánh đủ hàng cho tất cả sản phẩm
 *   "message": "...",    // mô tả trạng thái
 *   "items": [           // danh sách kết quả từng sản phẩm
 *     {
 *       "cartItemId": 5,
 *       "productName": "Bó hoa hồng đỏ",
 *       "needed": 2,
 *       "available": 10,
 *       "ok": true
 *     }, ...
 *   ]
 * }
 */
@RestController
@RequestMapping("/api/customer/pickup-check")
@RequiredArgsConstructor
public class PickupCheckController {

    private final CartItemRepository cartItemRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreRepository storeRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> check(
            Principal principal,
            @RequestParam("storeId") Long storeId,
            @RequestParam("itemIds") List<Long> itemIds) {

        Map<String, Object> response = new LinkedHashMap<>();

        // Kiểm tra chi nhánh tồn tại và đang hoạt động
        var storeOpt = storeRepository.findById(storeId);
        if (storeOpt.isEmpty() || !Boolean.TRUE.equals(storeOpt.get().getStatus())) {
            response.put("ok", false);
            response.put("message", "Chi nhánh này hiện không hoạt động.");
            response.put("items", List.of());
            return ResponseEntity.ok(response);
        }

        List<Map<String, Object>> itemResults = new ArrayList<>();
        boolean allOk = true;
        int shortfallCount = 0;

        for (Long cartItemId : itemIds) {
            Optional<CartItem> ciOpt = cartItemRepository.findById(cartItemId);
            if (ciOpt.isEmpty()) continue;

            CartItem ci = ciOpt.get();
            // Bảo vệ: chỉ cho phép kiểm tra giỏ của chính khách đang đăng nhập
            if (principal != null && ci.getCart() != null
                    && !ci.getCart().getUser().getUsername().equals(principal.getName())) {
                continue;
            }

            Long productId = ci.getProduct().getId();
            int needed = ci.getQuantity() != null ? ci.getQuantity() : 1;

            int available = storeInventoryRepository
                    .findByStoreIdAndProductId(storeId, productId)
                    .map(StoreInventory::getQuantity)
                    .orElse(0);

            boolean itemOk = available >= needed;
            if (!itemOk) {
                allOk = false;
                shortfallCount++;
            }

            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("cartItemId", cartItemId);
            itemMap.put("productName", ci.getProduct().getName());
            itemMap.put("needed", needed);
            itemMap.put("available", available);
            itemMap.put("ok", itemOk);
            if (!itemOk) {
                int shortage = needed - available;
                itemMap.put("reason", available == 0
                        ? "Hết hàng tại chi nhánh này"
                        : "Thiếu " + shortage + " sản phẩm (còn " + available + ")");
            }
            itemResults.add(itemMap);
        }

        response.put("ok", allOk);
        if (allOk) {
            response.put("message", "Có đủ sản phẩm đã chọn ✓");
        } else {
            response.put("message", shortfallCount == 1
                    ? "Có 1 sản phẩm không đủ hàng tại chi nhánh này."
                    : "Có " + shortfallCount + " sản phẩm không đủ hàng tại chi nhánh này.");
        }
        response.put("items", itemResults);

        return ResponseEntity.ok(response);
    }
}
