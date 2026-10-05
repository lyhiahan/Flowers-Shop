package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.repository.StyleValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProductAttributeService {
    private final StyleValueRepository values;
    @Transactional(readOnly=true)
    public List<StyleValue> activeValues() {
        return values.findByIsDeletedFalse().stream().filter(v->v.getStyle()!=null && !Boolean.TRUE.equals(v.getStyle().getIsDeleted()))
                .sorted(Comparator.comparing((StyleValue v)->v.getStyle().getName()).thenComparing(StyleValue::getName)).toList();
    }
    @Transactional(readOnly=true)
    public void assign(Product product, List<Long> ids, boolean submitted) {
        if (!submitted) return; // Legacy forms preserve previously assigned attributes.
        Set<Long> requested = ids==null ? Set.of() : new LinkedHashSet<>(ids);
        if (requested.size()>50 || requested.stream().anyMatch(id->id==null || id<=0))
            throw new IllegalArgumentException("Thuộc tính sản phẩm không hợp lệ.");
        var selected=values.findAllById(requested);
        if (selected.size()!=requested.size() || selected.stream().anyMatch(v->Boolean.TRUE.equals(v.getIsDeleted())
                || v.getStyle()==null || Boolean.TRUE.equals(v.getStyle().getIsDeleted())
                || (v.getStyle().getCategory()!=null && !v.getStyle().getCategory().getId().equals(product.getCategory().getId()))))
            throw new IllegalArgumentException("Thuộc tính đã bị xóa hoặc không thuộc danh mục sản phẩm.");
        product.setStyleValues(new LinkedHashSet<>(selected));
    }
}
