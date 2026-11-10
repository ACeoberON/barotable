package com.barotable.api;

import com.barotable.reservation.Reservation;
import com.barotable.reservation.WebReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 점주 화면 API
 *  PATCH /api/owner/reservations/{no}  상태 변경(방문/노쇼/취소/되돌리기), 테이블 이동
 * TODO: 점주 로그인(A-01)이 아직 화면 시연용이라 권한 검사가 없다. 점주 로그인 구현 때 함께 막을 것
 */
@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerApiController {

    private final WebReservationService service;

    public record Change(String status, String table) {}

    @PatchMapping("/reservations/{no}")
    @Transactional   // 이동과 상태 변경을 한 번에, 응답 만들 때 테이블 정보도 읽을 수 있게
    public Map<String, Object> change(@PathVariable String no, @RequestBody Change c) {
        Reservation r = null;
        if (c.table() != null) r = service.move(no, c.table());
        if (c.status() != null) r = service.changeStatus(no, ApiMaps.statusFromJs(c.status()));
        if (r == null) throw new IllegalArgumentException("바꿀 값이 없어요");
        return ApiMaps.reservation(r, true);
    }
}
