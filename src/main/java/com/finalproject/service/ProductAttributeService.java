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
    private final com.finalproject.repository.StyleRepository styles;

    @Transactional(readOnly=true)
    public Map<String, List<String>> inputSuggestions() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String group : List.of("Loại hoa", "Màu sắc", "Phong cách")) groups.put(group, new ArrayList<>());
        for (StyleValue value : activeValues()) {
            var target = groups.get(com.finalproject.utils.FlowerAttributes.group(value.getStyle().getName()));
            if (target != null && !target.contains(value.getName())) target.add(value.getName());
        }
        return groups;
    }

    @Transactional
    public void assignText(Product product, String types, String colors, String designs, boolean submitted) {
        if (!submitted) return;
        // Validate every group before changing existing assignments or creating suggestions.
        var inputs = List.of(com.finalproject.utils.FlowerAttributes.parse(types),
                com.finalproject.utils.FlowerAttributes.parse(colors), com.finalproject.utils.FlowerAttributes.parse(designs));
        var names = List.of("Loại hoa", "Màu sắc", "Phong cách");
        Set<StyleValue> selected = new LinkedHashSet<>();
        // Hidden legacy groups (e.g. shape/occasion) and omitted (null) fields must not disappear.
        for (StyleValue value : product.getStyleValues()) {
            if (value.getStyle() == null) continue;
            String g = com.finalproject.utils.FlowerAttributes.group(value.getStyle().getName());
            if (!com.finalproject.utils.FlowerAttributes.editable(value.getStyle().getName())) {
                selected.add(value);
            } else if (("Loại hoa".equals(g) && types == null)
                    || ("Màu sắc".equals(g) && colors == null)
                    || ("Phong cách".equals(g) && designs == null)) {
                selected.add(value);
            }
        }
        for (int index = 0; index < names.size(); index++) {
            if (inputs.get(index).isEmpty()) continue;
            String groupName = names.get(index);
            Style group = styles.findAll().stream()
                    .filter(s -> s.getCategory() == null && !Boolean.TRUE.equals(s.getIsDeleted())
                            && groupName.equals(com.finalproject.utils.FlowerAttributes.group(s.getName())))
                    .min(Comparator.comparing(Style::getId)).orElse(null);
            if (group == null) {
                if (styles.findAll().stream().anyMatch(s -> s.getName().equalsIgnoreCase(groupName)))
                    throw new IllegalArgumentException("Nhóm " + groupName + " chưa sẵn sàng. Vui lòng liên hệ quản trị viên.");
                group = styles.saveAndFlush(Style.builder().name(groupName).build());
            }
            // Serialize reuse/creation within an existing group, avoiding duplicate values on concurrent saves.
            group = styles.findLockedById(group.getId()).orElseThrow();
            if (Boolean.TRUE.equals(group.getIsDeleted())) throw new IllegalArgumentException("Nhóm thuộc tính đã bị ẩn.");
            var available = new ArrayList<>(values.findByStyleId(group.getId()).stream()
                    .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted())).toList());
            for (String text : inputs.get(index)) {
                StyleValue match = available.stream().filter(v ->
                        com.finalproject.utils.FlowerAttributes.normalize(v.getName()).equalsIgnoreCase(text))
                        .findFirst().orElse(null);
                if (match == null) {
                    match = values.save(StyleValue.builder().style(group).name(text).build());
                    available.add(match);
                }
                selected.add(match);
            }
        }
        product.setStyleValues(selected);
    }
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
        product.setStyleValues(validatedSelection(product, ids));
    }

    /** The restored selectors edit only the groups outside the three free-text fields. */
    @Transactional(readOnly=true)
    public void assignOther(Product product, List<Long> ids, boolean submitted) {
        if (!submitted) return;
        var selected = validatedSelection(product, ids);
        if (selected.stream().anyMatch(v -> com.finalproject.utils.FlowerAttributes.editable(v.getStyle().getName())))
            throw new IllegalArgumentException("Loại hoa, màu sắc và phong cách phải nhập ở ô tương ứng.");
        for (StyleValue value : product.getStyleValues()) {
            // Keep free-text groups and inactive values which are not available in the selectors.
            if (value.getStyle() == null || com.finalproject.utils.FlowerAttributes.editable(value.getStyle().getName())
                    || Boolean.TRUE.equals(value.getIsDeleted()) || Boolean.TRUE.equals(value.getStyle().getIsDeleted()))
                selected.add(value);
        }
        product.setStyleValues(selected);
    }

    private Set<StyleValue> validatedSelection(Product product, List<Long> ids) {
        Set<Long> requested = ids==null ? Set.of() : new LinkedHashSet<>(ids);
        if (requested.size()>50 || requested.stream().anyMatch(id->id==null || id<=0))
            throw new IllegalArgumentException("Thuộc tính sản phẩm không hợp lệ.");
        var selected=values.findAllById(requested);
        if (selected.size()!=requested.size() || selected.stream().anyMatch(v->Boolean.TRUE.equals(v.getIsDeleted())
                || v.getStyle()==null || Boolean.TRUE.equals(v.getStyle().getIsDeleted())
                || (v.getStyle().getCategory()!=null && product.getCatalogCategories().stream()
                    .noneMatch(c -> c.getId().equals(v.getStyle().getCategory().getId())))))
            throw new IllegalArgumentException("Thuộc tính đã bị xóa hoặc không thuộc danh mục sản phẩm.");
        return new LinkedHashSet<>(selected);
    }
}
