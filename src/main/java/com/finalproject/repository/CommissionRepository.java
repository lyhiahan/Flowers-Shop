package com.finalproject.repository;

import com.finalproject.entity.Commission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {
    List<Commission> findByIsDeletedFalse();
    List<Commission> findAllByOrderByIdAsc();
}
