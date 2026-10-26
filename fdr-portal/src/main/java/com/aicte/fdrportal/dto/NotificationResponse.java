package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.NotificationType;

import java.time.Instant;
import java.time.LocalDate;

public record NotificationResponse(
        Long id,
        Long fdrId,
        String instrumentReference,
        NotificationType type,
        String title,
        String message,
        LocalDate dueDate,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}