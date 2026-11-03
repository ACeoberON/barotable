package com.barotable.layout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findByStoreIdOrderBySortOrder(Long storeId);
}
