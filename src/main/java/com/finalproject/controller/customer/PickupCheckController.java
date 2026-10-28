package com.finalproject.controller.customer;

import com.finalproject.entity.CartItem;
import com.finalproject.entity.StoreInventory;
import com.finalproject.repository.CartItemRepository;
import com.finalproject.repository.StoreInventoryRepository;
import com.finalproject.repository.StoreRepository;
import com.finalproject.service.ProductAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/customer/pickup-check")
@RequiredArgsConstructor
public class PickupCheckController {
    private final CartItemRepository cartItemRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreRepository storeRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> check(Principal principal,
            @RequestParam("storeId") Long storeId, @RequestParam("itemIds") List<Long> itemIds) {
        if (principal == null) throw new AccessDeniedException("Vui lòng đăng nhập.");
        if (itemIds == null || itemIds.isEmpty() || itemIds.stream().anyMatch(id -> id == null || id <= 0))
            return invalid("Vui lòng chọn sản phẩm hợp lệ.");
        var store = storeRepository.findById(storeId).orElse(null);
        if (store == null || !Boolean.TRUE.equals(store.getStatus()) || !Boolean.TRUE.equals(store.getIsOpen()))
            return invalid("Chi nhánh hiện không nhận đơn.");

        List<CartItem> selected = new ArrayList<>();
        Map<Long, Long> neededByProduct = new LinkedHashMap<>();
        for (Long id : new LinkedHashSet<>(itemIds)) {
            var item = cartItemRepository.findById(id).orElse(null);
            if (item == null) return invalid("Sản phẩm đã chọn không còn trong giỏ.");
            if (item.getCart() == null || !principal.getName().equals(item.getCart().getUser().getUsername()))
                throw new AccessDeniedException("Bạn không có quyền truy cập giỏ hàng này.");
            if (item.getQuantity() == null || item.getQuantity() <= 0 || !ProductAvailability.isPublic(item.getProduct()))
                return invalid("Sản phẩm không còn khả dụng.");
            if (!ProductAvailability.supportsBranch(item.getProduct(), store))
                return invalid("Mẫu hoa '" + item.getProduct().getName() + "' chỉ bán tại chi nhánh "
                        + item.getProduct().getStore().getName() + ". Vui lòng chọn đúng chi nhánh nhận hàng.");
            selected.add(item);
            neededByProduct.merge(item.getProduct().getId(), item.getQuantity().longValue(), Long::sum);
        }
        Map<Long, Integer> stock = new HashMap<>();
        for (Long productId : neededByProduct.keySet()) {
            stock.put(productId, storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                    .filter(StoreInventory::isAvailableForSale)
                    .map(StoreInventory::getQuantity).map(q -> Math.max(0, q)).orElse(0));
        }
        boolean allOk = true;
        List<Map<String, Object>> results = new ArrayList<>();
        for (CartItem item : selected) {
            Long productId = item.getProduct().getId();
            long needed = neededByProduct.get(productId);
            int available = stock.get(productId);
            boolean ok = available >= needed;
            allOk &= ok;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cartItemId", item.getId()); row.put("productName", item.getProduct().getName());
            row.put("needed", needed); row.put("available", available); row.put("ok", ok);
            if (!ok) row.put("reason", "Tồn kho không đủ tổng số lượng đã chọn (cần " + needed + ", còn " + available + ").");
            results.add(row);
        }
        return ResponseEntity.ok(Map.of("ok", allOk, "items", results, "message",
                allOk ? "Có đủ sản phẩm đã chọn." : "Chi nhánh không đủ tổng số lượng đã chọn."));
    }

    private ResponseEntity<Map<String, Object>> invalid(String message) {
        return ResponseEntity.ok(Map.of("ok", false, "items", List.of(), "message", message));
    }
}
