package com.finalproject.utils;

import org.springframework.data.domain.*;
import org.springframework.ui.Model;
import java.util.function.Function;

public final class StorePages {
    private StorePages() {}
    public static <T> Page<T> load(int page, int size, Function<Pageable, Page<T>> query) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Trang phải từ 0 và kích thước trang từ 1 đến 100.");
        Page<T> result = query.apply(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        if (page > 0 && (result.isEmpty())) result = query.apply(PageRequest.of(Math.max(0, result.getTotalPages() - 1), size, Sort.by(Sort.Direction.DESC, "id")));
        return result;
    }
    public static void model(Model model, Page<?> page) {
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages", Math.max(1, page.getTotalPages()));
        model.addAttribute("totalElements", Math.toIntExact(page.getTotalElements()));
        model.addAttribute("pageSize", page.getSize());
    }
}
