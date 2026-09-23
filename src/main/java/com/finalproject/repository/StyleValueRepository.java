package com.finalproject.repository;

import com.finalproject.entity.StyleValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StyleValueRepository extends JpaRepository<StyleValue, Long> {
    List<StyleValue> findByStyleIdAndIsDeletedFalse(Long styleId);
    List<StyleValue> findByIsDeletedFalse();
}
