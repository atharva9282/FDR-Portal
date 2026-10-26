package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.MaturityState;
import com.aicte.fdrportal.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaturityRecordResponse(
        Long fdrId,
        Long institutionId,
        String institutionCode,
        String institutionName,
        String instrumentReference,
        String bankName,
        BigDecimal depositAmount,
        LocalDate issueDate,
        LocalDate maturityDate,
        long daysRemaining,
        MaturityState maturityState,
        InstrumentStatus status,
        VerificationStatus verificationStatus
) {
}