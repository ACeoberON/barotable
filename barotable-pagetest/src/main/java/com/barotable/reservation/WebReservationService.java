package com.barotable.reservation;

import com.barotable.account.Account;
import com.barotable.account.AccountRepository;
import com.barotable.layout.DiningTable;
import com.barotable.layout.DiningTableRepository;
import com.barotable.layout.Zone;
import com.barotable.store.StoreRule;
import com.barotable.store.StoreRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 손님 화면(예약하기, 조회, 취소)과 점주 화면(상태 변경, 테이블 이동)에서 쓰는 예약 처리.
 * 저장 순서는 ReservationService 와 같다: 테이블 행 잠금 → 규칙·겹침 확인 → 저장.
 */
@Service
@RequiredArgsConstructor
public class WebReservationService {

    public static final long STORE_ID = 1L;   // 지금은 매장 1곳(바로키친 성수점)

    private final DiningTableRepository tableRepository;
    private final StoreRuleRepository ruleRepository;
    private final ReservationRepository reservationRepository;
    private final AccountRepository accountRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_CHARS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** 화면에서 넘어오는 예약 내용 */
    public record Booking(String tableNo, LocalDate date, LocalTime time, int party,
                          String name, String phone, String email, String request) {}

    /** 손님 예약 저장. accountId 가 있으면 회원 예약, 없으면 비회원(조회 토큰 발급) */
    @Transactional
    public Reservation create(Booking b, Long accountId) {
        DiningTable found = tableRepository.findByStoreAndNo(STORE_ID, b.tableNo())
                .orElseThrow(() -> new IllegalArgumentException("없는 테이블입니다: " + b.tableNo()));
        // ① 테이블 행 잠금 (같은 테이블 예약끼리 순서대로 처리)
        DiningTable table = tableRepository.findByIdForUpdate(found.getId()).orElseThrow();
        StoreRule rule = ruleRepository.findById(STORE_ID).orElseThrow();

        // ② 규칙 확인
        if (!table.isActive()) throw new IllegalStateException("사용하지 않는 테이블입니다");
        if (b.party() < rule.getMinParty() || b.party() > rule.getMaxParty())
            throw new IllegalStateException("예약 가능 인원은 " + rule.getMinParty() + "~" + rule.getMaxParty() + "명입니다");
        if (b.party() > table.getSeats())
            throw new IllegalStateException(table.getSeats() + "인 테이블이라 " + b.party() + "명은 앉을 수 없어요");
        // 2명 이하는 6인 이상·단체·룸 테이블 불가 (큰 자리는 단체 손님용, 화면 BT.seatRule 과 같은 규칙)
        boolean bigSeat = table.getSeats() >= 6 || table.getShape() == com.barotable.layout.TableShape.ROOM
                || (table.getTags() != null && java.util.Arrays.asList(table.getTags().split(",")).contains("단체"));
        if (b.party() <= 2 && bigSeat) throw new IllegalStateException("단체석은 3명 이상부터 이용할 수 있어요");
        Zone z = table.getZone();
        if (z != null && z.getMinParty() != null && b.party() < z.getMinParty())
            throw new IllegalStateException(z.getName() + "은 " + z.getMinParty() + "명 이상만 받아요");
        if (z != null && z.getMaxParty() != null && b.party() > z.getMaxParty())
            throw new IllegalStateException(z.getName() + "은 " + z.getMaxParty() + "명까지만 받아요");
        LocalTime t = b.time();
        if (t.isBefore(rule.getOpenTime()) || !t.isBefore(rule.getCloseTime()))
            throw new IllegalStateException("영업시간(" + rule.getOpenTime() + "–" + rule.getCloseTime() + ") 밖이에요");
        if (rule.getBreakStart() != null && !t.isBefore(rule.getBreakStart()) && t.isBefore(rule.getBreakEnd()))
            throw new IllegalStateException(rule.getBreakStart() + "–" + rule.getBreakEnd() + "은 브레이크 타임이에요");
        if (LocalDateTime.of(b.date(), t).isBefore(LocalDateTime.now()))
            throw new IllegalStateException("이미 지난 시간이에요");

        // ③ 겹침 확인 (잠금을 잡은 뒤라 다른 요청이 끼어들 수 없다)
        LocalTime end = t.plusMinutes(rule.getDineMinutes());
        if (reservationRepository.countOverlaps(table.getId(), b.date(), t, end) > 0)
            throw new IllegalStateException("이미 예약된 시간입니다. 다른 시간이나 테이블을 골라 주세요");

        // 시간대별 한도 (운영조건.시간대한도, NULL 이면 제한 없음)
        if (rule.getSlotLimit() != null && reservationRepository.countSameSlot(STORE_ID, b.date(), t) >= rule.getSlotLimit())
            throw new IllegalStateException(t + "은 예약 한도(" + rule.getSlotLimit() + "팀)가 찼어요");

        // ④ 저장
        Account account = accountId == null ? null : accountRepository.findById(accountId).orElse(null);
        String token = account == null ? newToken() : null;
        LocalDateTime expires = account == null ? b.date().plusDays(1).atStartOfDay() : null;
        String request = b.request() == null || b.request().isBlank() ? null : b.request().trim();
        String email = b.email() == null || b.email().isBlank() ? null : b.email().trim();
        return reservationRepository.save(Reservation.web(nextNo(b.date()), table, account, b.name().trim(),
                b.phone().trim(), email, b.date(), t, rule.getDineMinutes(), b.party(), request, token, expires));
    }

    /** 점주 화면: 상태 변경 */
    @Transactional
    public Reservation changeStatus(String no, ReservationStatus status) {
        Reservation r = reservationRepository.findByReservationNo(no)
                .orElseThrow(() -> new IllegalArgumentException("없는 예약입니다: " + no));
        r.changeStatus(status);
        return r;
    }

    /** 점주 화면: 테이블 이동. 옮길 테이블을 잠그고 겹침을 다시 확인한다 */
    @Transactional
    public Reservation move(String no, String tableNo) {
        Reservation r = reservationRepository.findByReservationNo(no)
                .orElseThrow(() -> new IllegalArgumentException("없는 예약입니다: " + no));
        DiningTable found = tableRepository.findByStoreAndNo(STORE_ID, tableNo)
                .orElseThrow(() -> new IllegalArgumentException("없는 테이블입니다: " + tableNo));
        DiningTable table = tableRepository.findByIdForUpdate(found.getId()).orElseThrow();
        if (r.getParty() > table.getSeats())
            throw new IllegalStateException(table.getSeats() + "인 테이블이라 " + r.getParty() + "명은 앉을 수 없어요");
        if (reservationRepository.countOverlapsExcept(table.getId(), r.getVisitDate(), r.getStartTime(), r.getEndTime(), r.getId()) > 0)
            throw new IllegalStateException("그 테이블은 이 시간에 다른 예약이 있어요");
        r.moveTo(table);
        return r;
    }

    /** 손님 취소: 확정 상태이고 방문 시각 전일 때만 */
    @Transactional
    public Reservation cancelByGuest(Reservation r) {
        if (r.getStatus() == ReservationStatus.CANCELED) return r;
        if (r.getStatus() != ReservationStatus.BOOKED) throw new IllegalStateException("이미 방문 처리된 예약이라 취소할 수 없어요");
        if (!LocalDateTime.of(r.getVisitDate(), r.getStartTime()).isAfter(LocalDateTime.now()))
            throw new IllegalStateException("예약 시간이 지나 온라인으로 취소할 수 없어요. 매장으로 연락해 주세요");
        r.changeStatus(ReservationStatus.CANCELED);
        return r;
    }

    /** 예약번호: BT-날짜-그날 순번 4자리 (예약번호 칸 UNIQUE 라 겹치면 저장 단계에서 막힌다) */
    private String nextNo(LocalDate date) {
        String prefix = "BT-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        String max = reservationRepository.maxNoWithPrefix(prefix);
        int n = max == null ? 1 : Integer.parseInt(max.substring(prefix.length())) + 1;
        return prefix + String.format("%04d", n);
    }

    /** 조회 링크용 22자 무작위 값 (약 129비트) */
    static String newToken() {
        StringBuilder sb = new StringBuilder(22);
        for (int i = 0; i < 22; i++) sb.append(TOKEN_CHARS.charAt(RANDOM.nextInt(TOKEN_CHARS.length())));
        return sb.toString();
    }
}
