package com.barotable;

import com.barotable.reservation.ReservationRepository;
import com.barotable.reservation.ReservationService;
import com.barotable.reservation.WebReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** 두 손님이 같은 테이블, 같은 시간을 동시에 예약하면 한 명만 성공해야 한다 */
@SpringBootTest
class ReservationConcurrencyTest {

    @Autowired ReservationService reservationService;
    @Autowired WebReservationService webReservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate DATE = LocalDate.of(2026, 10, 24);

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from reservation where reservation_no like 'TEST-%'");
    }

    @Test
    @DisplayName("동시에 T03 19:00 예약 2건 → 1건만 저장, 1건은 거절")
    void onlyOneWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Callable<Void>> jobs = List.of("TEST-A", "TEST-B").stream().map(no -> (Callable<Void>) () -> {
            ready.await();
            try {
                reservationService.bookAsGuest(no, 3L, "손님" + no, "010-0000-0000", null, DATE, LocalTime.of(19, 0), 4);
                ok.incrementAndGet();
            } catch (IllegalStateException e) {
                rejected.incrementAndGet();
            }
            return null;
        }).toList();

        List<Future<Void>> futures = jobs.stream().map(pool::submit).toList();
        ready.countDown();          // 두 요청을 동시에 출발시킨다
        for (Future<Void> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(ok.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        assertThat(reservationRepository.countOverlaps(3L, DATE, LocalTime.of(19, 0), LocalTime.of(20, 30))).isEqualTo(1);
    }

    @Test
    void webCreateOnlyOneWins() throws Exception {
        LocalDate date = LocalDate.now().plusDays(31);
        String guestName = "TEST-" + UUID.randomUUID();
        WebReservationService.Booking booking = new WebReservationService.Booking(
                "T03", date, LocalTime.of(19, 0), 4, guestName, "010-0000-0000", null, null);
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<Boolean> book = () -> {
            start.await(10, TimeUnit.SECONDS);
            try {
                webReservationService.create(booking, null);
                return true;
            } catch (IllegalStateException e) {
                assertThat(e).hasMessage("이미 예약된 시간입니다. 다른 시간이나 테이블을 골라 주세요");
                return false;
            }
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = pool.submit(book);
            Future<Boolean> second = pool.submit(book);
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(jdbc.queryForObject("select count(*) from reservation where guest_name = ?", Integer.class, guestName))
                    .isEqualTo(1);
        } finally {
            jdbc.update("delete from table_use where reservation_id in (select reservation_id from reservation where guest_name = ?)", guestName);
            jdbc.update("delete from reservation where guest_name = ?", guestName);
        }
    }
}
