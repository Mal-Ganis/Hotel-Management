package com.hotelsystem.repository;

import com.hotelsystem.entity.PosConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PosConsumptionRepository extends JpaRepository<PosConsumption, Long> {
    List<PosConsumption> findByReservationId(Long reservationId);
    List<PosConsumption> findByConsumptionDateBetween(LocalDateTime start, LocalDateTime end);
    List<PosConsumption> findByCategory(String category);
}

