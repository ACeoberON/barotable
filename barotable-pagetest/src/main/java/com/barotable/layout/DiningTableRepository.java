package com.barotable.layout;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findByFloorIdAndActiveTrueOrderByTableNo(Long floorId);

    /** 예약 저장 전에 이 테이블 행을 잠근다 (SELECT ... FOR UPDATE). 같은 테이블 예약끼리 줄을 세운다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.id = :id")
    Optional<DiningTable> findByIdForUpdate(Long id);

    /** 화면의 테이블 번호(T03 등)로 찾기. 매장 안에서 번호가 겹치지 않는다고 보고 매장 기준으로 찾는다 */
    @Query("select t from DiningTable t where t.floor.store.id = :storeId and t.tableNo = :tableNo")
    Optional<DiningTable> findByStoreAndNo(Long storeId, String tableNo);

    List<DiningTable> findByFloorIdOrderByTableNo(Long floorId);
}
