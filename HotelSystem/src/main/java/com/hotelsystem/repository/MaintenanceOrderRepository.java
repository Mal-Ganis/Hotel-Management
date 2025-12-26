package com.hotelsystem.repository;

import com.hotelsystem.entity.MaintenanceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceOrderRepository extends JpaRepository<MaintenanceOrder, Long> {
    List<MaintenanceOrder> findByStatus(MaintenanceOrder.MaintenanceStatus status);
    List<MaintenanceOrder> findByRoomId(Long roomId);
    List<MaintenanceOrder> findByAssignedTo(String assignedTo);
}

