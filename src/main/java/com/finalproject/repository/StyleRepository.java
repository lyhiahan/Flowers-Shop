package com.finalproject.repository;

import com.finalproject.entity.Style;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StyleRepository extends JpaRepository<Style, Long> {
    List<Style> findByIsDeletedFalse();
    List<Style> findByCategoryIdAndIsDeletedFalse(Long categoryId);
}
