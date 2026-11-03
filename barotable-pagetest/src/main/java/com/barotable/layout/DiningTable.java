package com.barotable.layout;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테이블(손님이 앉는 곳). TABLE은 SQL 예약어라 dining_table 로 이름 붙였다.
 * 구역 밖 테이블도 있어서 zone 은 null 가능, 그래서 floor 를 직접 가진다.
 */
@Entity
@Table(name = "dining_table",
       uniqueConstraints = @UniqueConstraint(name = "uk_table_no", columnNames = {"floor_id", "table_no"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(name = "table_no", nullable = false, length = 10)
    private String tableNo;

    @Column(nullable = false)
    private int seats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableShape shape;

    @Column(length = 100)
    private String tags;

    @Column(nullable = false) private int x;
    @Column(nullable = false) private int y;
    @Column(nullable = false) private int w;
    @Column(nullable = false) private int h;

    @Column(nullable = false)
    private int rotation;

    /** 삭제 대신 숨김. 예약 기록이 있으면 FK 때문에 지울 수 없다 */
    @Column(nullable = false, columnDefinition = "tinyint(1)")
    private boolean active;
}
