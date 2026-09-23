package com.finalproject.repository;

import com.finalproject.entity.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {
    List<UserLevel> findByIsDeletedFalseOrderByMinPointAsc();
}
