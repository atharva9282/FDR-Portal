package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.ReleaseRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReleaseRequestResponse(
        Long id,
        Long fdrId,
        Long institutionId,
        String institutionName,
        String instrumentReference,
        BigDecimal depositAmount,
        LocalDate maturityDate,
        ReleaseRequestStatus status,
        String reason,
        String submittedBy,
        String reviewedBy,
        String remarks,
        String completionReference,
        Instant requestedAt,
        Instant updatedAt
) {
}