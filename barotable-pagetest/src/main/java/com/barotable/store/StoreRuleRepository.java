package com.barotable.store;

import org.springframework.data.jpa.repository.JpaRepository;

/** PK가 매장_ID라서 findById(매장_ID)로 그 매장의 운영조건을 찾는다 */
public interface StoreRuleRepository extends JpaRepository<StoreRule, Long> {
}
