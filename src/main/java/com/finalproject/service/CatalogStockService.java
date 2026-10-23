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
    private final com.finalproject.repository.ProductRepository products;

    public record Stock(long quantity, Long purchaseStoreId, boolean paused) {}

    @Transactional(readOnly = true)
    public Map<Long, Stock> summarize(Collection<Product> products, Long storeId) {
        List<Long> ids = products.stream().filter(Objects::nonNull).map(Product::getId)
                .filter(Objects::nonNull).distinct().toList();
        return summarizeIds(ids, storeId);
    }

    @Transactional(readOnly = true)
    public long availableQuantity(Long storeId) {
        return inventories.sumCatalogQuantity(storeId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Stock> summarizeIds(Collection<Long> productIds, Long storeId) {
        List<Long> ids = productIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        Map<Long, Stock> result = new LinkedHashMap<>();
        ids.forEach(id -> result.put(id, new Stock(0, null, false)));
        Set<Long> selling = new HashSet<>();
        Set<Long> paused = new HashSet<>();
        for (Product product : products.findAllById(ids)) {
            if (ProductAvailability.isVisible(product) && !Boolean.TRUE.equals(product.getIsSelling())
                    && (storeId == null || product.getStore() == null || storeId.equals(product.getStore().getId())))
                paused.add(product.getId());
        }
        for (var row : inventories.findCatalogStock(ids, storeId)) {
            if (Boolean.TRUE.equals(row.getPaused())) paused.add(row.getProductId());
            else selling.add(row.getProductId());
            Stock previous = result.get(row.getProductId());
            result.put(row.getProductId(), new Stock(
                    Math.addExact(previous.quantity(), Math.max(0, row.getQuantity().longValue())),
                    previous.purchaseStoreId() == null && row.getQuantity() > 0 ? row.getStoreId() : previous.purchaseStoreId(), false));
        }
        result.replaceAll((id, stock) -> new Stock(stock.quantity(), stock.purchaseStoreId(), paused.contains(id) && !selling.contains(id)));
        return result;
    }
}
