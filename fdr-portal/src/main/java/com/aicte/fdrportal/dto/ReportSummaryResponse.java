package com.aicte.fdrportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record ReportSummaryResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        long recordsSubmittedInPeriod,
        BigDecimal submittedDepositTotal,
        long pendingVerifications,
        long discrepancyCases,
        long activeRecords,
        long renewalPendingRecords,
        long releaseRequestedRecords,
        long releasedRecords,
        long maturityDatesInPeriod,
        Map<String, Long> currentStatusBreakdown
) {
}
