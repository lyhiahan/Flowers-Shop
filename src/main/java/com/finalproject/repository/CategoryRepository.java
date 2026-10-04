package com.finalproject.repository;

import com.finalproject.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @org.springframework.data.jpa.repository.Query("select c from Category c where c.isDeleted = false or c.isDeleted is null")
    java.util.List<Category> findActiveCategories();

    @org.springframework.data.jpa.repository.Query("select c from Category c where c.parent is null and (c.isDeleted = false or c.isDeleted is null)")
    java.util.List<Category> findActiveRootCategories();

    @org.springframework.data.jpa.repository.Query("select c from Category c where c.parent.id = :parentId and (c.isDeleted = false or c.isDeleted is null)")
    java.util.List<Category> findActiveChildren(@org.springframework.data.repository.query.Param("parentId") Long parentId);

    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    java.util.List<Category> findByParentIsNull();
    java.util.List<Category> findByParentId(Long parentId);
}
