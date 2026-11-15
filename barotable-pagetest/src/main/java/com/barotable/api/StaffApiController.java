package com.barotable.api;

import com.barotable.account.Account;
import com.barotable.account.AccountRepository;
import com.barotable.account.AccountStatus;
import com.barotable.account.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffApiController {

    public static final String SESSION_KEY = "staffId";
    public static final String ROLE_KEY = "staffRole";
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final AccountRepository accountRepository;

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        Account a = accountRepository.findByEmail(req.email().trim().toLowerCase()).orElse(null);
        boolean staff = a != null && (a.getRole() == Role.OWNER || a.getRole() == Role.ADMIN);
        if (!staff || !a.getPasswordHash().startsWith("$2") || !ENCODER.matches(req.password(), a.getPasswordHash()))
            return ResponseEntity.status(401).body(Map.of("message", "이메일 또는 비밀번호가 맞지 않아요."));
        if (a.getStatus() != AccountStatus.ACTIVE)
            return ResponseEntity.status(403).body(Map.of("message", "아직 승인되지 않은 계정이에요. 서비스 관리자 승인 후 로그인할 수 있어요."));
        HttpSession old = http.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = http.getSession(true);
        session.setAttribute(SESSION_KEY, a.getId());
        session.setAttribute(ROLE_KEY, a.getRole());
        return ResponseEntity.ok(Map.of("name", a.getName(), "role", a.getRole().name()));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        session.removeAttribute(ROLE_KEY);
        return Map.of("ok", true);
    }
}
