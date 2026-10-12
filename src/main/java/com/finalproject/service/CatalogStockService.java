package com.finalproject.service;

import com.finalproject.entity.Product;
import com.finalproject.repository.StoreInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogStockService {
    private final StoreInventoryRepository inventories;

    public record Stock(long quantity, Long purchaseStoreId) {}

    @Transactional(readOnly = true)
    public Map<Long, Stock> summarize(Collection<Product> products, Long storeId) {
        List<Long> ids = products.stream().filter(Objects::nonNull).map(Product::getId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        Map<Long, Stock> result = new LinkedHashMap<>();
        ids.forEach(id -> result.put(id, new Stock(0, null)));
        for (var row : inventories.findCatalogStock(ids, storeId)) {
            Stock previous = result.get(row.getProductId());
            result.put(row.getProductId(), new Stock(
                    Math.addExact(previous.quantity(), row.getQuantity().longValue()),
                    previous.purchaseStoreId() == null ? row.getStoreId() : previous.purchaseStoreId()));
        }
        return result;
    }
}
