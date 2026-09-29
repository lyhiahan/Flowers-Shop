package com.finalproject.repository;

import com.finalproject.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Store s where s.id = :id")
    Optional<Store> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Store> findByStatusTrue();
    org.springframework.data.domain.Page<Store> findByStatusTrue(org.springframework.data.domain.Pageable pageable);
    List<Store> findByOwnerId(Long ownerId);
    Optional<Store> findByName(String name);
    long countByStatus(Boolean status);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Store s WHERE s.status = true AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(s.address) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<Store> searchActiveStores(@org.springframework.data.repository.query.Param("kw") String kw);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Store s WHERE s.status = true AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(s.address) LIKE LOWER(CONCAT('%', :kw, '%')))")
    org.springframework.data.domain.Page<Store> searchActiveStores(@org.springframework.data.repository.query.Param("kw") String kw, org.springframework.data.domain.Pageable pageable);
}
