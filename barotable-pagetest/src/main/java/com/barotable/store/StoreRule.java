package com.barotable.store;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/** 운영조건: 매장과 1:1. 매장_ID를 그대로 PK로 쓴다(@MapsId) */
@Entity
@Table(name = "store_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreRule {

    @Id
    @Column(name = "store_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "break_start")
    private LocalTime breakStart;

    @Column(name = "break_end")
    private LocalTime breakEnd;

    @Column(name = "min_party", nullable = false)
    private int minParty;

    @Column(name = "max_party", nullable = false)
    private int maxParty;

    /** 한 팀이 자리를 쓰는 시간(분). 예약 종료시간 = 시작 + 이 값 */
    @Column(name = "dine_minutes", nullable = false)
    private int dineMinutes;

    /** 같은 시작 시각에 받을 수 있는 예약 수 (없으면 제한 없음) */
    @Column(name = "slot_limit")
    private Integer slotLimit;

    /** FALSE면 손님이 고르지 않고 추천 자리로 자동 배정 */
    @Column(name = "seat_choice", nullable = false, columnDefinition = "tinyint(1)")
    private boolean seatChoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_mode", nullable = false)
    private AiMode aiMode;
}
