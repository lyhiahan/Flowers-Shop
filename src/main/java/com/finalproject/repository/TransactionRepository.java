package com.finalproject.repository;

import com.finalproject.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    java.util.Optional<Transaction> findByRequestKey(String key);
    org.springframework.data.domain.Page<Transaction> findByStoreId(Long storeId, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Transaction t where t.id = :id")
    java.util.Optional<Transaction> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.List<Transaction> findAllByOrderIdAndType(Long orderId, String type);
    List<Transaction> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Transaction> findAllByOrderByCreatedAtDesc();
    org.springframework.data.domain.Page<Transaction> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Transaction> findByDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Transaction> findAllByOrderByCreatedAtDescIdDesc(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Transaction> findByDescriptionContainingIgnoreCaseOrderByCreatedAtDescIdDesc(String keyword, org.springframework.data.domain.Pageable pageable);
}
