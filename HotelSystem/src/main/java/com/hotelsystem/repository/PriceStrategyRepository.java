package com.hotelsystem.repository;

import com.hotelsystem.entity.PriceStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceStrategyRepository extends JpaRepository<PriceStrategy, Long> {
    List<PriceStrategy> findByIsActiveTrue();
    List<PriceStrategy> findByRoomTypeAndIsActiveTrue(String roomType);
    List<PriceStrategy> findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(
            LocalDate date1, LocalDate date2);
}

