package com.barotable.reservation;

import com.barotable.layout.DiningTable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 테이블이용 = 실제로 앉은 기록.
 * - reservation: 예약 손님이면 예약, 현장 손님이면 null. UNIQUE라 예약 1건에 최대 1번
 * - table: 실제로 앉은 테이블 (예약한 테이블과 다를 수 있음: 테이블 이동)
 * - endedAt: null 이면 지금 이용 중
 */
@Entity
@Table(name = "table_use")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TableUse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "use_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;

    @Column(nullable = false)
    private int party;

    @Column(name = "seated_at", nullable = false)
    private LocalDateTime seatedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(length = 200)
    private String memo;
}
