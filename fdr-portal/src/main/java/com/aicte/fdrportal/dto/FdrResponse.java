package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FdrResponse(
        Long id,
        Long institutionId,
        String institutionCode,
        String institutionName,
        String instrumentReference,
        String bankName,
        String branchName,
        BigDecimal depositAmount,
        LocalDate issueDate,
        LocalDate maturityDate,
        InstrumentStatus status,
        VerificationStatus verificationStatus,
        String verificationRemarks,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
