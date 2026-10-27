package com.aicte.fdrportal.dto;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Instant occurredAt,
        String actorEmail,
        String actorRole,
        String action,
        String targetType,
        String targetId,
        String outcome,
        String details
) {
}
