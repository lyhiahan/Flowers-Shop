package com.finalproject.repository;

import com.finalproject.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByIsDeletedFalseAndIsActiveTrue();
    List<Delivery> findByIsDeletedFalse();
    List<Delivery> findAllByOrderByIdAsc();
}
