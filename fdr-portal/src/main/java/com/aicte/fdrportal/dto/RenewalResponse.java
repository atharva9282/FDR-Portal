package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.RenewalRequestStatus;

import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RenewalResponse(
        Long id,
        Long originalFdrId,
        String originalReference,
        Long replacementFdrId,
        String replacementReference,
        String institutionName,
        BigDecimal depositAmount,
        LocalDate maturityDate,
        String replacementBankName,
        String replacementBranchName,
        BigDecimal replacementDepositAmount,
        LocalDate replacementIssueDate,
        LocalDate replacementMaturityDate,
        RenewalRequestStatus status,
        String submittedBy,
        String reviewedBy,
        String remarks,
        Instant createdAt,
        Instant updatedAt
) {
}