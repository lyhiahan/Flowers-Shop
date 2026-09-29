package com.finalproject.repository;

import com.finalproject.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Transaction t where t.id = :id")
    java.util.Optional<Transaction> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.List<Transaction> findAllByOrderIdAndType(Long orderId, String type);
    List<Transaction> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
