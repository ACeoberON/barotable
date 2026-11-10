package com.barotable.api;

import com.barotable.account.Account;
import com.barotable.account.AccountRepository;
import com.barotable.account.AccountStatus;
import com.barotable.account.Role;
import com.barotable.reservation.ReservationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 손님 회원 API (로그인 상태는 서버 세션에 계정 번호만 저장)
 *  POST /api/member/signup        회원가입 (권한 CUSTOMER, 비밀번호는 BCrypt 해시로 저장)
 *  POST /api/member/login         로그인
 *  POST /api/member/logout        로그아웃
 *  GET  /api/member/me            로그인한 회원 정보
 *  GET  /api/member/reservations  내 예약 (예약.계정_ID 로 조회)
 */
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberApiController {

    public static final String SESSION_KEY = "memberId";
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final AccountRepository accountRepository;
    private final ReservationRepository reservationRepository;

    public record SignupRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(min = 2, max = 50) String name,
            @NotBlank @Pattern(regexp = "01[016789]-?\\d{3,4}-?\\d{4}") String phone) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req, HttpServletRequest http) {
        String email = req.email().trim().toLowerCase();
        if (accountRepository.findByEmail(email).isPresent())
            return ResponseEntity.status(409).body(Map.of("message", "이미 가입된 이메일이에요."));
        Account a = accountRepository.save(Account.customer(email, ENCODER.encode(req.password()), req.name().trim(), req.phone().trim()));
        startSession(http, a);
        return ResponseEntity.ok(me(a));
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        Account a = accountRepository.findByEmail(req.email().trim().toLowerCase()).orElse(null);
        // 없는 계정·틀린 비밀번호·손님이 아닌 계정은 같은 문구로 (어느 쪽인지 알려 주지 않는다)
        if (a == null || a.getRole() != Role.CUSTOMER || a.getStatus() != AccountStatus.ACTIVE
                || !a.getPasswordHash().startsWith("$2") || !ENCODER.matches(req.password(), a.getPasswordHash()))
            return ResponseEntity.status(401).body(Map.of("message", "이메일 또는 비밀번호가 맞지 않아요."));
        startSession(http, a);
        return ResponseEntity.ok(me(a));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> current(HttpSession session) {
        Long id = (Long) session.getAttribute(SESSION_KEY);
        return id == null ? ResponseEntity.status(401).body(Map.of("message", "로그인이 필요해요."))
                : accountRepository.findById(id).<ResponseEntity<?>>map(a -> ResponseEntity.ok(me(a)))
                        .orElse(ResponseEntity.status(401).body(Map.of("message", "로그인이 필요해요.")));
    }

    @GetMapping("/reservations")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mine(HttpSession session) {
        Long id = (Long) session.getAttribute(SESSION_KEY);
        if (id == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요해요."));
        List<Map<String, Object>> list = reservationRepository.findByAccountIdOrderByVisitDateDescStartTimeDesc(id)
                .stream().map(r -> ApiMaps.reservation(r, true)).toList();
        return ResponseEntity.ok(list);
    }

    /** 로그인 성공 시 세션 번호를 새로 발급(세션 고정 공격 방지)하고 계정 번호만 저장 */
    private static void startSession(HttpServletRequest http, Account a) {
        HttpSession old = http.getSession(false);
        if (old != null) old.invalidate();
        http.getSession(true).setAttribute(SESSION_KEY, a.getId());
    }

    private static Map<String, Object> me(Account a) {
        return Map.of("id", a.getId(), "email", a.getEmail(), "name", a.getName(), "phone", a.getPhone() == null ? "" : a.getPhone());
    }
}
