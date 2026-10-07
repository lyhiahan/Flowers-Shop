package com.finalproject.service;

import com.finalproject.entity.Category;
import com.finalproject.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categories;
    private final jakarta.persistence.EntityManager entityManager;
    public boolean visible(Category category) {
        Set<Long> seen = new HashSet<>();
        for (Category current = category; current != null; current = current.getParent()) {
            if (Boolean.TRUE.equals(current.getIsDeleted()) || !seen.add(current.getId())) return false;
        }
        return category != null;
    }
    @Transactional(readOnly=true)
    public List<Category> descendants(Long id) {
        var all = categories.findActiveCategories().stream().filter(this::visible).toList();
        Set<Long> selected = new LinkedHashSet<>(); selected.add(id);
        boolean changed;
        do {
            changed = false;
            for (Category c : all) if (c.getParent() != null && selected.contains(c.getParent().getId()))
                changed |= selected.add(c.getId());
        } while (changed);
        return all.stream().filter(c -> selected.contains(c.getId()) && !c.getId().equals(id)).toList();
    }
    @Transactional
    public Category save(Long id, String name, String description, Long parentId) {
        // Both sides of simultaneous parent edits must see the latest tree before validating cycles.
        categories.findAllForUpdate().forEach(c -> entityManager.refresh(c, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE));
        if (name == null || name.isBlank() || name.trim().length() > 100 || (description != null && description.length() > 4000))
            throw new IllegalArgumentException("Tên danh mục bắt buộc, tối đa 100 ký tự; mô tả tối đa 4000 ký tự.");
        Category category = id == null ? Category.builder().isDeleted(false).build() : categories.findById(id).orElseThrow();
        if (Boolean.TRUE.equals(category.getIsDeleted())) throw new IllegalArgumentException("Khôi phục danh mục trước khi sửa.");
        Category parent = null;
        if (parentId != null && parentId > 0) {
            parent = categories.findById(parentId).filter(this::visible).orElseThrow(() -> new IllegalArgumentException("Danh mục cha không còn hoạt động."));
            Set<Long> visited = new HashSet<>();
            for (Category c = parent; c != null; c = c.getParent()) {
                if (Objects.equals(c.getId(), id) || !visited.add(c.getId())) throw new IllegalArgumentException("Không được tạo vòng lặp danh mục cha–con.");
            }
        }
        category.setName(name.trim()); category.setDescription(description == null ? null : description.trim());
        category.setParent(parent); return categories.save(category);
    }
}
