package com.barotable.layout;

import com.barotable.store.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 층: 매장 1 : 층 N */
@Entity
@Table(name = "floor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "floor_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
