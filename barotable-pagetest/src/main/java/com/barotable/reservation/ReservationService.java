package com.barotable.reservation;

import com.barotable.layout.DiningTable;
import com.barotable.layout.DiningTableRepository;
import com.barotable.store.StoreRule;
import com.barotable.store.StoreRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 예약 저장. 중복 예약 방지 순서:
 * ① 트랜잭션 시작 → ② 테이블 행 잠금(SELECT ... FOR UPDATE) → ③ 겹침 다시 확인 → ④ 저장 → ⑤ 커밋
 * 같은 테이블을 동시에 예약하려는 두 번째 요청은 ②에서 기다렸다가 ③에서 겹침을 발견해 거절된다.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final DiningTableRepository tableRepository;
    private final StoreRuleRepository ruleRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation bookAsGuest(String reservationNo, Long tableId, String name, String phone, String email,
                                   LocalDate date, LocalTime start, int party) {
        return book(reservationNo, tableId, name, phone, email, date, start, party, true, 0);
    }

    /**
     * 시연용: lock=false 면 잠금 없이 확인만 하고, holdMillis 만큼 확인과 저장 사이를 늦춘다.
     * (잠금이 왜 필요한지 화면에서 보여 주기 위한 것. 실제 예약은 bookAsGuest 를 쓴다)
     */
    @Transactional
    public Reservation book(String reservationNo, Long tableId, String name, String phone, String email,
                            LocalDate date, LocalTime start, int party, boolean lock, long holdMillis) {
        DiningTable table = (lock ? tableRepository.findByIdForUpdate(tableId) : tableRepository.findById(tableId))
                .orElseThrow(() -> new IllegalArgumentException("없는 테이블입니다: " + tableId));
        if (!table.isActive()) throw new IllegalStateException("사용하지 않는 테이블입니다");
        if (party > table.getSeats()) throw new IllegalStateException("좌석수보다 인원이 많습니다");

        StoreRule rule = ruleRepository.findById(table.getFloor().getStore().getId())
                .orElseThrow(() -> new IllegalStateException("운영조건이 없는 매장입니다"));
        LocalTime end = start.plusMinutes(rule.getDineMinutes());

        if (reservationRepository.countOverlaps(tableId, date, start, end) > 0) {
            throw new IllegalStateException("이미 예약된 시간입니다");
        }
        if (holdMillis > 0) {
            try { Thread.sleep(holdMillis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return reservationRepository.save(
                Reservation.guest(reservationNo, table, name, phone, email, date, start, rule.getDineMinutes(), party,
                        null, null));
    }
}
