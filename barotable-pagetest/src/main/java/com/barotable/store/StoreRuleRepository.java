package com.barotable.store;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** PK가 매장_ID라서 findById(매장_ID)로 그 매장의 운영조건을 찾는다 */
public interface StoreRuleRepository extends JpaRepository<StoreRule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StoreRule r where r.id = :storeId")
    Optional<StoreRule> findByIdForUpdate(Long storeId);
}
