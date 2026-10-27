package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.ReportSummaryResponse;
import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.VerificationStatus;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class ReportingService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final FdrInstrumentRepository fdrRepository;

    public ReportingService(FdrInstrumentRepository fdrRepository) {
        this.fdrRepository = fdrRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse summary(LocalDate from, LocalDate to) {
        validateRange(from, to);

        List<FdrInstrument> submitted = findSubmittedBetween(from, to);
        List<FdrInstrument> maturing = fdrRepository.findByMaturityDateBetween(from, to);

        Map<String, Long> currentStatuses = new java.util.TreeMap<>();
        for (InstrumentStatus status : InstrumentStatus.values()) {
            long count = submitted.stream()
                    .filter(fdr -> fdr.getStatus() == status)
                    .count();
            currentStatuses.put(status.name(), count);
        }

        BigDecimal depositTotal = submitted.stream()
                .map(FdrInstrument::getDepositAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pending = submitted.stream()
                .filter(fdr -> isVerificationPending(fdr.getVerificationStatus()))
                .count();
        long discrepancies = submitted.stream()
                .filter(fdr -> fdr.getVerificationStatus()
                        == VerificationStatus.DISCREPANCY_FOUND)
                .count();
        long active = submitted.stream()
                .filter(fdr -> fdr.getStatus() == InstrumentStatus.ACTIVE)
                .count();
        long renewalPending = submitted.stream()
                .filter(fdr -> fdr.getStatus() == InstrumentStatus.RENEWAL_PENDING)
                .count();
        long releaseRequested = submitted.stream()
                .filter(fdr -> fdr.getStatus() == InstrumentStatus.RELEASE_REQUESTED)
                .count();
        long released = submitted.stream()
                .filter(fdr -> fdr.getStatus() == InstrumentStatus.RELEASED)
                .count();

        return new ReportSummaryResponse(
                from,
                to,
                Instant.now(),
                submitted.size(),
                depositTotal,
                pending,
                discrepancies,
                active,
                renewalPending,
                releaseRequested,
                released,
                maturing.stream().filter(fdr -> !isTerminal(fdr.getStatus())).count(),
                currentStatuses
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<FdrInstrument> instruments = findSubmittedBetween(from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("fdr_id,institution_code,institution_name,instrument_reference,bank_name,branch_name,deposit_amount,issue_date,maturity_date,status,verification_status,submitted_at\r\n");

        for (FdrInstrument fdr : instruments) {
            csv.append(fdr.getId()).append(',')
                    .append(csv(fdr.getInstitution().getInstitutionCode())).append(',')
                    .append(csv(fdr.getInstitution().getName())).append(',')
                    .append(csv(fdr.getInstrumentReference())).append(',')
                    .append(csv(fdr.getBankName())).append(',')
                    .append(csv(fdr.getBranchName())).append(',')
                    .append(fdr.getDepositAmount()).append(',')
                    .append(fdr.getIssueDate()).append(',')
                    .append(fdr.getMaturityDate()).append(',')
                    .append(fdr.getStatus()).append(',')
                    .append(fdr.getVerificationStatus()).append(',')
                    .append(fdr.getSubmittedAt())
                    .append("\r\n");
        }

        return csv.toString();
    }

    private List<FdrInstrument> findSubmittedBetween(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant().minusNanos(1);
        return fdrRepository.findBySubmittedAtBetween(start, end);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both 'from' and 'to' dates are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must be on or before 'to' date");
        }
        if (from.isBefore(LocalDate.of(2000, 1, 1)) || to.isAfter(LocalDate.now(BUSINESS_ZONE).plusYears(1))) {
            throw new IllegalArgumentException("Report dates are outside the supported range");
        }
    }

    private boolean isVerificationPending(VerificationStatus status) {
        return status == VerificationStatus.PENDING
                || status == VerificationStatus.UNDER_REVIEW
                || status == VerificationStatus.EXTERNAL_CONFIRMATION_PENDING;
    }

    private boolean isTerminal(InstrumentStatus status) {
        return status == InstrumentStatus.RELEASED
                || status == InstrumentStatus.REJECTED
                || status == InstrumentStatus.RENEWED;
    }

    /**
     * Quote every textual field and neutralize spreadsheet formula prefixes.
     * Spreadsheet applications may interpret a quoted CSV field as a formula,
     * so CSV quoting alone is not a sufficient defense.
     */
    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }

        int firstMeaningfulCharacter = 0;
        while (firstMeaningfulCharacter < value.length()) {
            char ch = value.charAt(firstMeaningfulCharacter);
            if (!Character.isWhitespace(ch) && !Character.isISOControl(ch)) {
                break;
            }
            firstMeaningfulCharacter++;
        }

        String safeValue = value;
        if (firstMeaningfulCharacter < value.length()) {
            char first = value.charAt(firstMeaningfulCharacter);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                safeValue = "'" + value;
            }
        }

        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
