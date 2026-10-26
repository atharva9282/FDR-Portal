package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.VerificationMethod;
import com.aicte.fdrportal.enums.VerificationOutcome;

import java.time.Instant;

public record VerificationReviewResponse(
        Long id,
        Long fdrId,
        String reviewerName,
        VerificationOutcome outcome,
        VerificationMethod verificationMethod,
        String sourceReference,
        String remarks,
        Instant reviewedAt
) {
}