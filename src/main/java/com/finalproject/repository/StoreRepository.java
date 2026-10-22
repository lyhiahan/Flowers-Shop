package com.finalproject.repository;

import com.finalproject.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Store> {
    Optional<Store> findByCreationRequestKey(String key);
    @org.springframework.data.jpa.repository.Query("select s from Store s left join s.owner o where o.id = :userId or s.id = :legacyId or exists (select m.id from StoreMembership m where m.store = s and m.user.id = :userId and m.active = true)")
    org.springframework.data.domain.Page<Store> findAccessible(Long userId, Long legacyId, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Store s where s.id = :id")
    Optional<Store> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Store> findByStatusTrue();
    org.springframework.data.domain.Page<Store> findByStatusTrue(org.springframework.data.domain.Pageable pageable);
    List<Store> findByOwnerId(Long ownerId);
    Optional<Store> findByName(String name);
    Optional<Store> findFirstByNameOrderByIdAsc(String name);
    long countByStatus(Boolean status);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Store s WHERE s.status = true AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(s.address) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<Store> searchActiveStores(@org.springframework.data.repository.query.Param("kw") String kw);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.owner LEFT JOIN FETCH s.storeLevel")
    List<Store> findAllWithDetails();

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Store s WHERE s.status = true AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(s.address) LIKE LOWER(CONCAT('%', :kw, '%')))")
    org.springframework.data.domain.Page<Store> searchActiveStores(@org.springframework.data.repository.query.Param("kw") String kw, org.springframework.data.domain.Pageable pageable);
}
