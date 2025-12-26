package com.hotelsystem.repository;

import com.hotelsystem.entity.RoomCheckInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomCheckInspectionRepository extends JpaRepository<RoomCheckInspection, Long> {
    Optional<RoomCheckInspection> findByReservationId(Long reservationId);
}

