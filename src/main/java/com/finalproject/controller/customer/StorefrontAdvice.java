package com.finalproject.controller.customer;

import com.finalproject.entity.Store;
import com.finalproject.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;

@ControllerAdvice(basePackages = "com.finalproject.controller.customer",
        assignableTypes = com.finalproject.controller.common.NotificationController.class)
@RequiredArgsConstructor
public class StorefrontAdvice {
    private final StoreRepository stores;

    @ModelAttribute("footerStores")
    public List<Store> footerStores() {
        return stores.findByStatusTrue().stream().sorted(java.util.Comparator.comparing(Store::getId)).toList();
    }
}
