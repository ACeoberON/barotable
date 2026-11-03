package com.barotable.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** 점주 화면: 우리 매장 그날 예약 (인덱스 store_id, visit_date) */
    List<Reservation> findByStoreIdAndVisitDateOrderByStartTime(Long storeId, LocalDate visitDate);

    /** 회원: 내 예약 */
    List<Reservation> findByAccountIdOrderByVisitDateDescStartTimeDesc(Long accountId);

    /** 비회원: 메일 링크의 조회 토큰 */
    Optional<Reservation> findByLookupToken(String lookupToken);

    /**
     * 겹치는 예약 수. 같은 테이블, 같은 날, 확정·방문 상태 중
     * 기존.시작 < 새.종료 AND 새.시작 < 기존.종료 이면 겹친다 (끝나는 시각 = 다음 시작은 허용)
     */
    @Query("""
            select count(r) from Reservation r
            where r.table.id = :tableId and r.visitDate = :date
              and r.status in (com.barotable.reservation.ReservationStatus.BOOKED,
                               com.barotable.reservation.ReservationStatus.VISITED)
              and r.startTime < :end and :start < r.endTime
            """)
    long countOverlaps(Long tableId, LocalDate date, LocalTime start, LocalTime end);
}
