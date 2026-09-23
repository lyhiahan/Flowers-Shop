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
}