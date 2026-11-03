package com.barotable.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableUseRepository extends JpaRepository<TableUse, Long> {

    /** 지금 이 테이블에 앉아 있는 손님 (종료일시가 비어 있음) */
    List<TableUse> findByTableIdAndEndedAtIsNull(Long tableId);

    Optional<TableUse> findByReservationId(Long reservationId);
}
