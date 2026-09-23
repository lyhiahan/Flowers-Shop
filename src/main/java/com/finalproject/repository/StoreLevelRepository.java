package com.finalproject.repository;

import com.finalproject.entity.StoreLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreLevelRepository extends JpaRepository<StoreLevel, Long> {
    List<StoreLevel> findByIsDeletedFalseOrderByMinPointAsc();
}
