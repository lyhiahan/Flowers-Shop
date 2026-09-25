package com.finalproject.repository;

import com.finalproject.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByStatusTrue();
    List<Store> findByOwnerId(Long ownerId);
    Optional<Store> findByName(String name);
    long countByStatus(Boolean status);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Store s WHERE s.status = true AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(s.address) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<Store> searchActiveStores(@org.springframework.data.repository.query.Param("kw") String kw);
}