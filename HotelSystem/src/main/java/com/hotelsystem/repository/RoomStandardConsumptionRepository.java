package com.hotelsystem.repository;

import com.hotelsystem.entity.RoomStandardConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomStandardConsumptionRepository extends JpaRepository<RoomStandardConsumption, Long> {
    List<RoomStandardConsumption> findByRoomId(Long roomId);
    List<RoomStandardConsumption> findByInventoryId(Long inventoryId);
}

