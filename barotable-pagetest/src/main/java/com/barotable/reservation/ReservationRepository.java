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

    /** 테이블 이동용: 자기 자신(exceptId)은 빼고 겹치는 예약 수 */
    @Query("""
            select count(r) from Reservation r
            where r.table.id = :tableId and r.visitDate = :date and r.id <> :exceptId
              and r.status in (com.barotable.reservation.ReservationStatus.BOOKED,
                               com.barotable.reservation.ReservationStatus.VISITED)
              and r.startTime < :end and :start < r.endTime
            """)
    long countOverlapsExcept(Long tableId, LocalDate date, LocalTime start, LocalTime end, Long exceptId);

    Optional<Reservation> findByReservationNo(String reservationNo);

    /** 매장의 전체 예약 (화면 첫 로딩용, 날짜·시간 순) */
    List<Reservation> findByStoreIdAndVisitDateGreaterThanEqualOrderByVisitDateAscStartTimeAsc(Long storeId, LocalDate from);

    /** 그날 이 번호(BT-YYYYMMDD-)로 시작하는 예약번호 중 가장 큰 값 */
    @Query("select max(r.reservationNo) from Reservation r where r.reservationNo like concat(:prefix, '%')")
    String maxNoWithPrefix(String prefix);

    /** 시간대별 한도: 같은 매장·날짜·시작 시각의 유효 예약(확정·방문) 수 */
    @Query("""
            select count(r) from Reservation r
            where r.store.id = :storeId and r.visitDate = :date and r.startTime = :time
              and r.status in (com.barotable.reservation.ReservationStatus.BOOKED,
                               com.barotable.reservation.ReservationStatus.VISITED)
            """)
    long countSameSlot(Long storeId, LocalDate date, LocalTime time);
}
