package com.barotable.reservation;

import com.barotable.account.Account;
import com.barotable.layout.DiningTable;
import com.barotable.store.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 예약 = 약속. "몇 월 며칠 몇 시에 몇 명이 어느 테이블에 오기로 했다"
 * - account: 회원이면 계정, 비회원이면 null (대신 lookupToken 으로 조회)
 * - store: 테이블에서 알 수 있지만 점주 화면 조회용으로 일부러 저장(예약 생성 때 테이블의 매장으로 채움)
 * - endTime: 시작 + 이용시간. 겹침 검사용으로 저장
 */
@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @Column(name = "reservation_no", nullable = false, unique = true, length = 20)
    private String reservationNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "guest_name", nullable = false, length = 50)
    private String guestName;

    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;

    @Column(name = "guest_email", length = 100)
    private String guestEmail;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(length = 200)
    private String request;

    /** 비회원 메일 조회 링크용 22자 무작위 값. 회원 예약이면 null */
    @Column(name = "lookup_token", unique = true, columnDefinition = "char(22)")
    private String lookupToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 비회원 예약 만들기 (테스트·서비스에서 사용) */
    public static Reservation guest(String reservationNo, DiningTable table, String name, String phone, String email,
                                    LocalDate date, LocalTime start, int dineMinutes, int party,
                                    String token, LocalDateTime tokenExpiresAt) {
        Reservation r = new Reservation();
        r.reservationNo = reservationNo;
        r.table = table;
        r.store = table.getFloor().getStore();   // 매장_ID는 손님 값이 아니라 테이블에서 채운다
        r.guestName = name;
        r.guestPhone = phone;
        r.guestEmail = email;
        r.visitDate = date;
        r.startTime = start;
        r.endTime = start.plusMinutes(dineMinutes);
        r.party = party;
        r.channel = ReservationChannel.WEB;
        r.status = ReservationStatus.BOOKED;
        r.lookupToken = token;
        r.tokenExpiresAt = tokenExpiresAt;
        return r;
    }
}
