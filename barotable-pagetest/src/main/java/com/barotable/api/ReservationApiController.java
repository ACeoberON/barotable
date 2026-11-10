package com.barotable.api;

import com.barotable.reservation.Reservation;
import com.barotable.reservation.ReservationRepository;
import com.barotable.reservation.WebReservationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

/**
 * 손님 화면 API
 *  POST /api/reservations              예약 저장 (C-04 '예약 확정하기')
 *  GET  /api/reservations/lookup?t=    메일 조회 링크로 예약 보기 (C-07)
 *  POST /api/reservations/lookup       예약번호 + 연락처로 예약 보기 (C-07 보조)
 *  POST /api/reservations/{no}/cancel  손님 취소 (토큰 또는 연락처, 회원은 로그인 세션으로 본인 확인)
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationApiController {

    private final WebReservationService service;
    private final ReservationRepository reservationRepository;

    public record CreateRequest(
            @NotBlank String table,
            @NotNull LocalDate date,
            @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String time,
            @Min(1) @Max(20) int party,
            @NotBlank @Size(min = 2, max = 50) String name,
            @NotBlank @Pattern(regexp = "01[016789]-?\\d{3,4}-?\\d{4}") String phone,
            @Email @Size(max = 100) String email,
            @Size(max = 200) String request) {}

    @PostMapping
    @Transactional
    public Map<String, Object> create(@Valid @RequestBody CreateRequest req, HttpSession session) {
        Long memberId = (Long) session.getAttribute(MemberApiController.SESSION_KEY);
        Reservation r = service.create(new WebReservationService.Booking(req.table(), req.date(), LocalTime.parse(req.time()),
                req.party(), req.name(), req.phone(), req.email(), req.request()), memberId);
        Map<String, Object> m = new java.util.LinkedHashMap<>(ApiMaps.reservation(r, true));
        m.put("token", r.getLookupToken());   // 비회원만 값이 있다 (실서비스에서는 메일로만 보낸다)
        return m;
    }

    @GetMapping("/lookup")
    @Transactional(readOnly = true)
    public ResponseEntity<?> byToken(@RequestParam("t") String token) {
        Optional<Reservation> r = reservationRepository.findByLookupToken(token);
        if (r.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "조회 링크가 올바르지 않아요. 아래에서 이메일로 새 링크를 받아 주세요."));
        if (r.get().getTokenExpiresAt().isBefore(LocalDateTime.now()))
            return ResponseEntity.status(410).body(Map.of("message", "방문일이 지나 만료된 조회 링크예요. 지난 예약은 매장으로 문의해 주세요."));
        return ResponseEntity.ok(ApiMaps.reservation(r.get(), true));
    }

    public record NoPhone(@NotBlank String no, @NotBlank String phone) {}

    @PostMapping("/lookup")
    @Transactional(readOnly = true)
    public ResponseEntity<?> byNoAndPhone(@Valid @RequestBody NoPhone req) {
        return findByNoAndPhone(req.no(), req.phone())
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(ApiMaps.reservation(r, true)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "예약번호와 연락처가 맞는 예약이 없어요. 다시 확인해 주세요.")));
    }

    public record CancelRequest(String token, String phone) {}

    @PostMapping("/{no}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable String no, @RequestBody CancelRequest req, HttpSession session) {
        Optional<Reservation> found = reservationRepository.findByReservationNo(no);
        Long memberId = (Long) session.getAttribute(MemberApiController.SESSION_KEY);
        boolean allowed = found.isPresent() && (
                (req.token() != null && req.token().equals(found.get().getLookupToken()))
                || (req.phone() != null && digits(req.phone()).equals(digits(found.get().getGuestPhone())))
                || (memberId != null && found.get().getAccount() != null && memberId.equals(found.get().getAccount().getId())));
        if (!allowed) return ResponseEntity.status(404).body(Map.of("message", "예약을 찾을 수 없어요."));
        return ResponseEntity.ok(ApiMaps.reservation(service.cancelByGuest(found.get()), true));
    }

    private Optional<Reservation> findByNoAndPhone(String no, String phone) {
        return reservationRepository.findByReservationNo(no.trim().toUpperCase())
                .filter(r -> digits(r.getGuestPhone()).equals(digits(phone)));
    }

    private static String digits(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }
}
