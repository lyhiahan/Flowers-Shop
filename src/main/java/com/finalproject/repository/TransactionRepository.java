package com.finalproject.repository;

import com.finalproject.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
