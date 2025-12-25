package com.hotelsystem.repository;

import com.hotelsystem.entity.OtaOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtaOrderRepository extends JpaRepository<OtaOrder, Long> {
    Optional<OtaOrder> findByOtaOrderId(String otaOrderId);
    List<OtaOrder> findByOtaPlatform(String otaPlatform);
    List<OtaOrder> findBySyncStatus(OtaOrder.SyncStatus syncStatus);
}

