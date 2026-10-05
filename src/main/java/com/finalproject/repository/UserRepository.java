package com.finalproject.repository;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {
    @org.springframework.data.jpa.repository.Query("select distinct u from User u left join u.roles r left join u.store assigned where u.isActive = true " +
            "and (u.accountDisabledByAdmin = false or u.accountDisabledByAdmin is null) and (u.role = :role or r = :role) " +
            "and (exists (select s.id from Store s where s.id = :storeId and s.owner = u) " +
            "or exists (select m.id from StoreMembership m where m.user = u and m.store.id = :storeId and m.active = true) " +
            "or (assigned.id = :storeId and not exists (select old.id from StoreMembership old where old.user = u and old.store.id = :storeId)))")
    List<User> findChatManagers(Long storeId, Role role);
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id <> :ownerId and (u.store.id = :storeId or exists (select m.id from StoreMembership m where m.user = u and m.store.id = :storeId and m.active = true))")
    org.springframework.data.domain.Page<User> findStoreStaff(Long storeId, Long ownerId, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByStoreIdAndRole(Long storeId, Role role);
    List<User> findByStoreId(Long storeId);
    long countByRole(Role role);
    List<User> findByRole(Role role);
    org.springframework.data.domain.Page<User> findByRole(Role role, org.springframework.data.domain.Pageable pageable);

    List<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String fullName, String email);
    org.springframework.data.domain.Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String fullName, String email, org.springframework.data.domain.Pageable pageable);

    List<User> findByCreatedDateBetween(java.time.LocalDate start, java.time.LocalDate end);
}
