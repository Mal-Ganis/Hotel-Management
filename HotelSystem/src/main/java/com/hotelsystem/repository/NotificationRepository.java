package com.hotelsystem.repository;

import com.hotelsystem.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientTypeAndRecipientId(String recipientType, String recipientId);
    List<Notification> findByRecipientTypeAndRecipientIdAndStatus(String recipientType, String recipientId, Notification.NotificationStatus status);
    long countByRecipientTypeAndRecipientIdAndStatus(String recipientType, String recipientId, Notification.NotificationStatus status);
}

