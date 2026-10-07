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
    public Map<String,List<StyleValue>> catalogGroups() {
        Map<String,List<StyleValue>> groups = new LinkedHashMap<>();
        for (String key : List.of("Kiểu dáng", "Dịp tặng", "Loại hoa", "Màu sắc")) groups.put(key, new ArrayList<>());
        for (StyleValue value : activeValues()) {
            String key = switch (value.getStyle().getName().trim().toLowerCase(Locale.ROOT)) {
                case "kiểu dáng", "kiểu dáng cắm", "kiểu dáng hoa", "phong cách cắm" -> "Kiểu dáng";
                case "màu sắc", "tone màu hoa", "tone màu thiết kế" -> "Màu sắc";
                case "dịp tặng" -> "Dịp tặng";
                case "loại hoa" -> "Loại hoa";
                default -> value.getStyle().getName().trim();
            };
            if (groups.containsKey(key)) groups.get(key).add(value);
        }
        return groups;
    }
    @Transactional(readOnly=true)
    public List<StyleValue> activeValues() {
        return values.findAll().stream().filter(v->!Boolean.TRUE.equals(v.getIsDeleted()) && v.getStyle()!=null && !Boolean.TRUE.equals(v.getStyle().getIsDeleted()))
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
