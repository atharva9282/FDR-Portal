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
    private final InstitutionAccessService accessService;

    public NotificationService(
            NotificationRepository notificationRepository,
            InstitutionAccessService accessService) {
        this.notificationRepository = notificationRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAll(boolean unreadOnly) {

        Long institutionId = accessService.institutionScopeOrNull();

        List<Notification> notifications;
        if (institutionId == null) {
            notifications = unreadOnly
                    ? notificationRepository.findByReadFalseOrderByCreatedAtDesc()
                    : notificationRepository.findAllByOrderByCreatedAtDesc();
        } else {
            notifications = unreadOnly
                    ? notificationRepository
                            .findByFdr_Institution_IdAndReadFalseOrderByCreatedAtDesc(institutionId)
                    : notificationRepository
                            .findAllByFdr_Institution_IdOrderByCreatedAtDesc(institutionId);
        }

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

        accessService.assertCanAccessFdr(notification.getFdr());

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