package com.finalproject.repository;

import com.finalproject.entity.Style;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StyleRepository extends JpaRepository<Style, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Style s where s.id = :id")
    java.util.Optional<Style> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Style> findByIsDeletedFalse();
    List<Style> findByCategoryIdAndIsDeletedFalse(Long categoryId);
}
