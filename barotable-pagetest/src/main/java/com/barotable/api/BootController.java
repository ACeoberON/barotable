package com.barotable.api;

import com.barotable.layout.*;
import com.barotable.reservation.Reservation;
import com.barotable.reservation.ReservationRepository;
import com.barotable.reservation.ReservationStatus;
import com.barotable.reservation.WebReservationService;
import com.barotable.store.Store;
import com.barotable.store.StoreRepository;
import com.barotable.store.StoreRule;
import com.barotable.store.StoreRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * 화면이 처음 열릴 때 필요한 값(매장, 운영조건, 좌석도, 예약)을 DB에서 읽어
 * "window.BT_DB = {...}" 형태의 스크립트로 내려 준다.
 * static/js/db-sync.js 가 이 값을 하드코딩 데이터(data.js) 위에 덮어써서,
 * 기존 화면 코드는 그대로 두고 DB 값으로 그려지게 한다.
 *
 * scope=owner : 점주 화면용. 예약자 이름·연락처·요청사항까지 포함
 * 그 외(손님 화면) : 빈자리 계산에 필요한 값(테이블·시간·인원·상태)만 포함
 */
@RestController
@RequiredArgsConstructor
public class BootController {

    private static final long STORE_ID = WebReservationService.STORE_ID;
    private static final String[] ZONE_COLORS = {"teal", "slate", "amber", "indigo"};
    private static final Map<FacilityKind, String> FACILITY_LABEL = Map.of(
            FacilityKind.WINDOW, "창문", FacilityKind.WALL, "", FacilityKind.ENTRANCE, "입구",
            FacilityKind.KITCHEN, "주방", FacilityKind.RESTROOM, "화장실", FacilityKind.STAIRS, "계단");

    private final StoreRepository storeRepository;
    private final StoreRuleRepository ruleRepository;
    private final FloorRepository floorRepository;
    private final ZoneRepository zoneRepository;
    private final DiningTableRepository tableRepository;
    private final FacilityRepository facilityRepository;
    private final ReservationRepository reservationRepository;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/api/boot.js", produces = "application/javascript;charset=UTF-8")
    @Transactional(readOnly = true)
    public ResponseEntity<String> boot(@RequestParam(defaultValue = "guest") String scope) throws Exception {
        boolean owner = "owner".equals(scope);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("store", store());
        out.put("floors", floors());
        out.put("reservations", reservations(owner));
        if (owner) out.put("noShowHistory", noShowHistory());
        String js = "window.BT_DB = " + objectMapper.writeValueAsString(out) + ";\n";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.valueOf("application/javascript;charset=UTF-8")).body(js);
    }

    private Map<String, Object> store() {
        Store s = storeRepository.findById(STORE_ID).orElseThrow();
        StoreRule r = ruleRepository.findById(STORE_ID).orElseThrow();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", s.getName());
        m.put("address", s.getAddress());
        m.put("phone", s.getPhone());
        Map<String, String> hours = new LinkedHashMap<>();
        hours.put("open", hhmm(r.getOpenTime()));
        hours.put("close", hhmm(r.getCloseTime()));
        // 브레이크가 없으면 시작=끝으로 보내 화면에서 '없음'으로 표시
        hours.put("breakStart", r.getBreakStart() == null ? hhmm(r.getCloseTime()) : hhmm(r.getBreakStart()));
        hours.put("breakEnd", r.getBreakEnd() == null ? hhmm(r.getCloseTime()) : hhmm(r.getBreakEnd()));
        m.put("hours", hours);
        m.put("dineMinutes", r.getDineMinutes());
        m.put("minParty", r.getMinParty());
        m.put("maxParty", r.getMaxParty());
        m.put("slotLimit", r.getSlotLimit());
        m.put("allowSeatChoice", r.isSeatChoice());
        m.put("ai", r.getAiMode().name().toLowerCase());
        return m;
    }

    private List<Map<String, Object>> floors() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Floor f : floorRepository.findByStoreIdOrderBySortOrder(STORE_ID)) {
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("id", "F" + f.getId());
            fm.put("name", f.getName());

            List<Map<String, Object>> zones = new ArrayList<>();
            int i = 0;
            for (Zone z : zoneRepository.findByFloorId(f.getId())) {
                Map<String, Object> zm = new LinkedHashMap<>();
                zm.put("id", "z" + z.getId());
                zm.put("name", z.getName());
                zm.put("x", z.getX()); zm.put("y", z.getY()); zm.put("w", z.getW()); zm.put("h", z.getH());
                zm.put("color", ZONE_COLORS[i++ % ZONE_COLORS.length]);
                zm.put("minParty", z.getMinParty());
                zm.put("maxParty", z.getMaxParty());
                zones.add(zm);
            }
            fm.put("zones", zones);

            List<Map<String, Object>> fixtures = new ArrayList<>();
            for (Facility x : facilityRepository.findByFloorId(f.getId())) {
                Map<String, Object> xm = new LinkedHashMap<>();
                xm.put("id", "fx" + x.getId());
                xm.put("type", x.getKind().name().toLowerCase());
                xm.put("label", FACILITY_LABEL.get(x.getKind()));
                xm.put("x", x.getX()); xm.put("y", x.getY()); xm.put("w", x.getW()); xm.put("h", x.getH());
                fixtures.add(xm);
            }
            fm.put("fixtures", fixtures);

            List<Map<String, Object>> tables = new ArrayList<>();
            for (DiningTable t : tableRepository.findByFloorIdAndActiveTrueOrderByTableNo(f.getId())) {
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("id", t.getTableNo());
                tm.put("seats", t.getSeats());
                tm.put("shape", switch (t.getShape()) { case ROUND -> "round"; case SQUARE -> "rect"; case BAR -> "bar"; case ROOM -> "room"; });
                tm.put("x", t.getX()); tm.put("y", t.getY()); tm.put("w", t.getW()); tm.put("h", t.getH());
                tm.put("zone", t.getZone() == null ? null : "z" + t.getZone().getId());
                tm.put("tags", t.getTags() == null || t.getTags().isBlank() ? List.of() : Arrays.stream(t.getTags().split(",")).map(String::trim).toList());
                tm.put("combinable", false);
                tables.add(tm);
            }
            fm.put("tables", tables);
            out.add(fm);
        }
        return out;
    }

    /** 지난 7일부터 앞으로의 예약 */
    private List<Map<String, Object>> reservations(boolean owner) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Reservation r : reservationRepository
                .findByStoreIdAndVisitDateGreaterThanEqualOrderByVisitDateAscStartTimeAsc(STORE_ID, LocalDate.now().minusDays(7))) {
            if (r.getReservationNo().startsWith("DEMO-")) continue;   // DB 확인 화면의 시연 예약은 빼고
            out.add(ApiMaps.reservation(r, owner));
        }
        return out;
    }

    /** 연락처별 누적 노쇼 횟수 (점주 화면 경고용) */
    private Map<String, Long> noShowHistory() {
        Map<String, Long> m = new TreeMap<>();
        for (Reservation r : reservationRepository.findAll())
            if (r.getStatus() == ReservationStatus.NO_SHOW) m.merge(r.getGuestPhone(), 1L, Long::sum);
        return m;
    }

    private static String hhmm(LocalTime t) { return t == null ? null : String.format("%02d:%02d", t.getHour(), t.getMinute()); }
}
