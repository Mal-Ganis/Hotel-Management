package com.hotelsystem.service;

import com.hotelsystem.entity.Notification;
import com.hotelsystem.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createNotification(String title, String content, Notification.NotificationType type,
                                          String recipientType, String recipientId,
                                          String relatedEntityType, Long relatedEntityId) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRecipientType(recipientType);
        notification.setRecipientId(recipientId);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setStatus(Notification.NotificationStatus.UNREAD);
        
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByRecipient(String recipientType, String recipientId) {
        return notificationRepository.findByRecipientTypeAndRecipientId(recipientType, recipientId);
    }

    public List<Notification> getUnreadNotificationsByRecipient(String recipientType, String recipientId) {
        return notificationRepository.findByRecipientTypeAndRecipientIdAndStatus(
                recipientType, recipientId, Notification.NotificationStatus.UNREAD);
    }

    public long getUnreadCount(String recipientType, String recipientId) {
        return notificationRepository.countByRecipientTypeAndRecipientIdAndStatus(
                recipientType, recipientId, Notification.NotificationStatus.UNREAD);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));
        
        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
}

