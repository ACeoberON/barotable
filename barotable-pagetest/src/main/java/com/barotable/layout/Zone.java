package com.barotable.layout;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 구역: 창가, 바 카운터처럼 테이블을 묶는 영역. 인원 조건은 자리 추천 때 쓴다 */
@Entity
@Table(name = "zone")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "min_party")
    private Integer minParty;

    @Column(name = "max_party")
    private Integer maxParty;

    /** 층 캔버스(1000 x 600) 위 위치와 크기 */
    @Column(nullable = false) private int x;
    @Column(nullable = false) private int y;
    @Column(nullable = false) private int w;
    @Column(nullable = false) private int h;
}
