package com.barotable.api;

import com.barotable.reservation.Reservation;
import com.barotable.reservation.ReservationStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/** 엔티티 → 화면(JS)이 쓰는 모양. 상태 이름은 화면 쪽 값(booked/visited/noshow/cancelled)으로 바꾼다 */
final class ApiMaps {
    private ApiMaps() {}

    static String statusToJs(ReservationStatus s) {
        return switch (s) { case BOOKED -> "booked"; case VISITED -> "visited"; case NO_SHOW -> "noshow"; case CANCELED -> "cancelled"; };
    }

    static ReservationStatus statusFromJs(String s) {
        return switch (s) {
            case "booked" -> ReservationStatus.BOOKED;
            case "visited" -> ReservationStatus.VISITED;
            case "noshow" -> ReservationStatus.NO_SHOW;
            case "cancelled" -> ReservationStatus.CANCELED;
            default -> throw new IllegalArgumentException("알 수 없는 상태: " + s);
        };
    }

    /** full=true 면 예약자 정보까지, false 면 빈자리 계산용 값만 */
    static Map<String, Object> reservation(Reservation r, boolean full) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("no", r.getReservationNo());
        m.put("date", r.getVisitDate().toString());
        m.put("time", String.format("%02d:%02d", r.getStartTime().getHour(), r.getStartTime().getMinute()));
        m.put("party", r.getParty());
        m.put("table", r.getTable().getTableNo());
        m.put("status", statusToJs(r.getStatus()));
        if (full) {
            m.put("name", r.getGuestName());
            m.put("phone", r.getGuestPhone());
            m.put("email", r.getGuestEmail() == null ? "" : r.getGuestEmail());
            m.put("request", r.getRequest() == null ? "" : r.getRequest());
            m.put("member", r.getAccount() != null);
            m.put("channel", r.getChannel().name());
        } else {
            m.put("name", "");
            m.put("phone", "");
            m.put("email", "");
            m.put("request", "");
        }
        return m;
    }
}
