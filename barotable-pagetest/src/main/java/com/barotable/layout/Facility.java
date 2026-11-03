package com.barotable.layout;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 시설물: 창문, 입구, 주방 등. 좌석도에 그리기만 하고 예약 대상이 아니다 */
@Entity
@Table(name = "facility")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "facility_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityKind kind;

    @Column(nullable = false) private int x;
    @Column(nullable = false) private int y;
    @Column(nullable = false) private int w;
    @Column(nullable = false) private int h;
}
