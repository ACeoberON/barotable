package com.barotable.devcheck;

import com.barotable.account.AccountRepository;
import com.barotable.layout.*;
import com.barotable.reservation.*;
import com.barotable.store.Store;
import com.barotable.store.StoreRepository;
import com.barotable.store.StoreRule;
import com.barotable.store.StoreRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * DB 확인 화면(/dev/db)에 보여 줄 값을 JPA Repository 로 읽어 온다.
 * 화면에서 지연 로딩 오류가 나지 않도록, 트랜잭션 안에서 필요한 값만 꺼내 record 로 넘긴다.
 */
@Service
@RequiredArgsConstructor
public class DbCheckService {

    private final JdbcTemplate jdbc;
    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;
    private final StoreRuleRepository storeRuleRepository;
    private final FloorRepository floorRepository;
    private final ZoneRepository zoneRepository;
    private final DiningTableRepository tableRepository;
    private final FacilityRepository facilityRepository;
    private final ReservationRepository reservationRepository;
    private final TableUseRepository tableUseRepository;
    private final ReservationService reservationService;

    public static final long STORE_ID = 1L;

    // ---------- 화면에 넘기는 값 ----------
    public record Conn(String product, String version, String database, String url) {}
    public record Count(String entity, String table, String korean, long rows) {}
    public record StoreInfo(String name, String address, String owner, String hours, String breakTime,
                            String party, int dineMinutes, String slotLimit, boolean seatChoice, String aiMode) {}
    public record TableRow(Long id, String no, int seats, String zone, String shape, boolean active) {}
    public record FloorRow(String name, List<TableRow> tables, long facilities) {}
    public record ResRow(String no, String time, String table, int party, String name, String status,
                         String who, String channel, boolean seated) {}
    public record RaceLog(String who, String requested, String answered, String waited, String result, boolean ok) {}

    @Transactional(readOnly = true)
    public Conn connection() {
        String version = jdbc.queryForObject("select version()", String.class);
        String db = jdbc.queryForObject("select database()", String.class);
        String url = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getURL());
        String product = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName());
        return new Conn(product, version, db, url == null ? "" : url.replaceAll("\\?.*", ""));
    }

    @Transactional(readOnly = true)
    public List<Count> counts() {
        return List.of(
                new Count("Account", "account", "계정", accountRepository.count()),
                new Count("Store", "store", "매장", storeRepository.count()),
                new Count("StoreRule", "store_rule", "운영조건", storeRuleRepository.count()),
                new Count("Floor", "floor", "층", floorRepository.count()),
                new Count("Zone", "zone", "구역", zoneRepository.count()),
                new Count("DiningTable", "dining_table", "테이블", tableRepository.count()),
                new Count("Facility", "facility", "시설물", facilityRepository.count()),
                new Count("Reservation", "reservation", "예약", reservationRepository.count()),
                new Count("TableUse", "table_use", "테이블이용", tableUseRepository.count()));
    }

    @Transactional(readOnly = true)
    public StoreInfo store() {
        Store s = storeRepository.findById(STORE_ID).orElseThrow();
        StoreRule r = storeRuleRepository.findById(STORE_ID).orElseThrow();
        String brk = r.getBreakStart() == null ? "없음" : r.getBreakStart() + " – " + r.getBreakEnd();
        return new StoreInfo(s.getName(), s.getAddress(), s.getOwner().getName() + " (" + s.getOwner().getEmail() + ")",
                r.getOpenTime() + " – " + r.getCloseTime(), brk, r.getMinParty() + " – " + r.getMaxParty() + "명",
                r.getDineMinutes(), r.getSlotLimit() == null ? "제한 없음" : r.getSlotLimit() + "건",
                r.isSeatChoice(), r.getAiMode().name());
    }

    @Transactional(readOnly = true)
    public List<FloorRow> floors() {
        List<FloorRow> out = new ArrayList<>();
        for (Floor f : floorRepository.findByStoreIdOrderBySortOrder(STORE_ID)) {
            List<TableRow> rows = tableRepository.findAll().stream()
                    .filter(t -> t.getFloor().getId().equals(f.getId()))
                    .sorted(Comparator.comparing(DiningTable::getTableNo))
                    .map(t -> new TableRow(t.getId(), t.getTableNo(), t.getSeats(),
                            t.getZone() == null ? "(구역 밖)" : t.getZone().getName(), t.getShape().name(), t.isActive()))
                    .toList();
            out.add(new FloorRow(f.getName(), rows, facilityRepository.findByFloorId(f.getId()).size()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ResRow> reservations(LocalDate date) {
        return reservationRepository.findByStoreIdAndVisitDateOrderByStartTime(STORE_ID, date).stream()
                .map(r -> new ResRow(r.getReservationNo(),
                        r.getStartTime() + " – " + r.getEndTime(), r.getTable().getTableNo(), r.getParty(),
                        r.getGuestName(), r.getStatus().name(),
                        r.getAccount() == null ? "비회원" : "회원 #" + r.getAccount().getId(),
                        r.getChannel().name(), tableUseRepository.findByReservationId(r.getId()).isPresent()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResRow> memberReservations(Long accountId) {
        return reservationRepository.findByAccountIdOrderByVisitDateDescStartTimeDesc(accountId).stream()
                .map(r -> new ResRow(r.getReservationNo(), r.getVisitDate() + " " + r.getStartTime(),
                        r.getTable().getTableNo(), r.getParty(), r.getGuestName(), r.getStatus().name(),
                        "회원 #" + accountId, r.getChannel().name(), false))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ResRow> tokenLookup(String token) {
        return reservationRepository.findByLookupToken(token)
                .map(r -> new ResRow(r.getReservationNo(), r.getVisitDate() + " " + r.getStartTime(),
                        r.getTable().getTableNo(), r.getParty(), r.getGuestName(), r.getStatus().name(),
                        "비회원", r.getChannel().name(), false));
    }

    @Transactional(readOnly = true)
    public long overlaps(Long tableId, LocalDate date, LocalTime start) {
        int dine = storeRuleRepository.findById(STORE_ID).orElseThrow().getDineMinutes();
        return reservationRepository.countOverlaps(tableId, date, start, start.plusMinutes(dine));
    }

    /** 예약 한 건 저장 (실제 서비스와 같은 방식: 테이블 행 잠금 → 겹침 확인 → 저장) */
    public String book(Long tableId, LocalDate date, LocalTime start, int party, String name) {
        String no = demoNo("");
        try {
            reservationService.bookAsGuest(no, tableId, name, "010-0000-0000", null, date, start, party);
            return "저장됨: " + no;
        } catch (RuntimeException e) {
            return "거절: " + e.getMessage();
        }
    }

    /** 두 손님(A, B)이 같은 테이블·시간을 동시에 예약. A는 확인 후 저장 전에 2초 머문다 */
    public List<RaceLog> race(Long tableId, LocalDate date, LocalTime start, int party, boolean lock) throws Exception {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        // 같은 자리로 다시 시연할 수 있게, 이전 시연 예약(DEMO-) 중 같은 테이블·날짜·시각은 먼저 지운다
        jdbc.update("delete from reservation where reservation_no like 'DEMO-%' and table_id = ? and visit_date = ? and start_time = ?",
                tableId, date, start);
        String base = demoNo("");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<RaceLog> a = () -> attempt("손님 A", base + "A", tableId, date, start, party, lock, 2000, f);
            Callable<RaceLog> b = () -> { Thread.sleep(300); return attempt("손님 B", base + "B", tableId, date, start, party, lock, 0, f); };
            Future<RaceLog> fa = pool.submit(a), fb = pool.submit(b);
            return List.of(fa.get(20, TimeUnit.SECONDS), fb.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdown();
        }
    }

    private RaceLog attempt(String who, String no, Long tableId, LocalDate date, LocalTime start, int party,
                            boolean lock, long hold, DateTimeFormatter f) {
        long t0 = System.currentTimeMillis();
        String req = java.time.LocalTime.now().format(f);
        try {
            reservationService.book(no, tableId, who, "010-0000-0000", null, date, start, party, lock, hold);
            long ms = System.currentTimeMillis() - t0;
            return new RaceLog(who, req, java.time.LocalTime.now().format(f), String.format("%.1f초", ms / 1000.0), "저장됨 (" + no + ")", true);
        } catch (RuntimeException e) {
            long ms = System.currentTimeMillis() - t0;
            return new RaceLog(who, req, java.time.LocalTime.now().format(f), String.format("%.1f초", ms / 1000.0), "거절: " + e.getMessage(), false);
        }
    }

    /** 이 화면에서 만든 예약(DEMO-로 시작)만 지운다. 시험 데이터는 그대로 */
    @Transactional
    public int reset() {
        return jdbc.update("delete from reservation where reservation_no like 'DEMO-%'");
    }

    private static String demoNo(String suffix) {
        // DEMO- + 월일시분초밀리초(13자리) = 18자, 뒤에 A/B 붙여도 20자 이내
        return "DEMO-" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmssSSS")) + suffix;
    }
}
