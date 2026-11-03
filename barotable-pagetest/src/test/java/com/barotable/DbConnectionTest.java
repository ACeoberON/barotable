package com.barotable;

import com.barotable.layout.DiningTableRepository;
import com.barotable.reservation.Reservation;
import com.barotable.reservation.ReservationRepository;
import com.barotable.reservation.TableUseRepository;
import com.barotable.store.StoreRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** MySQL에 실제로 연결해서 ERD v2 대로 만들어졌는지 확인한다. 각 테스트는 끝나면 되돌린다(@Transactional) */
@SpringBootTest
@Transactional
class DbConnectionTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired DiningTableRepository tableRepository;
    @Autowired StoreRuleRepository ruleRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired TableUseRepository tableUseRepository;

    static final LocalDate OCT10 = LocalDate.of(2026, 10, 10);

    @Test
    @DisplayName("테이블 9개와 외래키 12개가 만들어졌다")
    void schema() {
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database()", Integer.class);
        Integer fks = jdbc.queryForObject(
                "select count(*) from information_schema.referential_constraints where constraint_schema = database()", Integer.class);
        assertThat(tables).isEqualTo(9);
        assertThat(fks).isEqualTo(12);
    }

    @Test
    @DisplayName("시험 데이터: 1층 테이블 11개, 운영조건 이용시간 90분")
    void seed() {
        assertThat(tableRepository.findByFloorIdAndActiveTrueOrderByTableNo(1L)).hasSize(11);
        assertThat(ruleRepository.findById(1L)).get().extracting("dineMinutes").isEqualTo(90);
    }

    @Test
    @DisplayName("회원은 계정_ID로 내 예약 2건을 찾는다")
    void memberReservations() {
        List<Reservation> mine = reservationRepository.findByAccountIdOrderByVisitDateDescStartTimeDesc(4L);
        assertThat(mine).hasSize(2);
    }

    @Test
    @DisplayName("비회원은 조회 토큰으로 예약을 찾는다")
    void guestLookup() {
        assertThat(reservationRepository.findByLookupToken("aB3dE5fG7hJ9kL1mN3pQ5r"))
                .get().extracting("guestName").isEqualTo("정하늘");
    }

    @Test
    @DisplayName("점주 화면: 매장 1의 10월 10일 예약 4건")
    void ownerDashboard() {
        assertThat(reservationRepository.findByStoreIdAndVisitDateOrderByStartTime(1L, OCT10)).hasSize(4);
    }

    @Test
    @DisplayName("겹침: T09(19:00~20:30)에 19:30 시작은 겹치고, 20:30 시작은 안 겹친다")
    void overlap() {
        assertThat(reservationRepository.countOverlaps(9L, OCT10, LocalTime.of(19, 30), LocalTime.of(21, 0))).isEqualTo(1);
        assertThat(reservationRepository.countOverlaps(9L, OCT10, LocalTime.of(20, 30), LocalTime.of(22, 0))).isZero();
    }

    @Test
    @DisplayName("예약 5번은 착석 기록 1건이 있다 (예약 1 : 이용 0..1)")
    void tableUse() {
        assertThat(tableUseRepository.findByReservationId(5L)).isPresent();
    }

    @Test
    @DisplayName("예약이 있는 테이블은 지울 수 없다 (FK) → 사용여부로 숨겨야 한다")
    void cannotDeleteTableWithReservations() {
        assertThatThrownBy(() -> {
            tableRepository.deleteById(9L);
            tableRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
