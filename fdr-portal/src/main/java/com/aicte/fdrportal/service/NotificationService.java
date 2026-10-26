package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.NotificationResponse;
import com.aicte.fdrportal.entity.Notification;
import com.aicte.fdrportal.exception.ResourceNotFoundException;
import com.aicte.fdrportal.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAll(boolean unreadOnly) {

        List<Notification> notifications = unreadOnly
                ? notificationRepository
                .findByReadFalseOrderByCreatedAtDesc()
                : notificationRepository
                .findAllByOrderByCreatedAtDesc();

        return notifications.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with ID: "
                                + notificationId));

        notification.markAsRead();

        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(
            Notification notification) {

        return new NotificationResponse(
                notification.getId(),
                notification.getFdr().getId(),
                notification.getFdr().getInstrumentReference(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getDueDate(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}