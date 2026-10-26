package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.RenewalRequestStatus;

import java.time.Instant;

public record RenewalResponse(
        Long id,
        Long originalFdrId,
        String originalReference,
        Long replacementFdrId,
        String replacementReference,
        RenewalRequestStatus status,
        String submittedBy,
        String reviewedBy,
        String remarks,
        Instant createdAt,
        Instant updatedAt
) {
}