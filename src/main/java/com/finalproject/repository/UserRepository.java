package com.finalproject.repository;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    List<User> findByStoreIdAndRole(Long storeId, Role role);
    List<User> findByStoreId(Long storeId);
    long countByRole(Role role);
    List<User> findByRole(Role role);

    List<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String fullName, String email);

    List<User> findByCreatedDateBetween(java.time.LocalDate start, java.time.LocalDate end);
}