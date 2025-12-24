package com.hotelsystem.repository;

import com.hotelsystem.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    
    Page<OperationLog> findByActionContainingIgnoreCase(String action, Pageable pageable);
    
    Page<OperationLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
