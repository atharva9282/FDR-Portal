package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.MaturityRecordResponse;
import com.aicte.fdrportal.dto.MonitoringDashboardResponse;
import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.MaturityState;
import com.aicte.fdrportal.enums.VerificationStatus;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;
import com.aicte.fdrportal.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class MonitoringService {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final FdrInstrumentRepository fdrRepository;
    private final NotificationRepository notificationRepository;
    private final InstitutionAccessService accessService;
    private final int expiringSoonDays;

    public MonitoringService(
            FdrInstrumentRepository fdrRepository,
            NotificationRepository notificationRepository,
            InstitutionAccessService accessService,
            @Value("${app.monitoring.expiring-soon-days:30}")
            int expiringSoonDays) {

        this.fdrRepository = fdrRepository;
        this.notificationRepository = notificationRepository;
        this.accessService = accessService;
        this.expiringSoonDays = expiringSoonDays;
    }

    @Transactional(readOnly = true)
    public MonitoringDashboardResponse getDashboard(int windowDays) {

        validateWindow(windowDays);

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate windowEnd = today.plusDays(windowDays);

        List<FdrInstrument> instruments = findAuthorizedInstruments();

        long active = instruments.stream()
                .filter(fdr -> fdr.getStatus() == InstrumentStatus.ACTIVE)
                .filter(fdr -> !fdr.getMaturityDate().isBefore(today))
                .count();

        long expiring = instruments.stream()
                .filter(fdr -> !isTerminal(fdr.getStatus()))
                .filter(fdr -> !fdr.getMaturityDate().isBefore(today))
                .filter(fdr -> !fdr.getMaturityDate().isAfter(windowEnd))
                .count();

        long matured = instruments.stream()
                .filter(fdr -> !isTerminal(fdr.getStatus()))
                .filter(fdr -> fdr.getMaturityDate().isBefore(today))
                .count();

        long renewed = instruments.stream()
                .filter(fdr -> fdr.getStatus() == InstrumentStatus.RENEWED)
                .count();

        long released = instruments.stream()
                .filter(fdr -> fdr.getStatus() == InstrumentStatus.RELEASED)
                .count();

        long pendingVerification = instruments.stream()
                .filter(fdr -> isVerificationPending(
                        fdr.getVerificationStatus()))
                .count();

        long discrepancies = instruments.stream()
                .filter(fdr -> fdr.getVerificationStatus()
                        == VerificationStatus.DISCREPANCY_FOUND)
                .count();

        return new MonitoringDashboardResponse(
                windowDays,
                instruments.size(),
                active,
                expiring,
                matured,
                renewed,
                released,
                pendingVerification,
                discrepancies,
                countUnreadNotifications()
        );
    }

    @Transactional(readOnly = true)
    public List<MaturityRecordResponse> findMaturities(int withinDays) {

        validateWindow(withinDays);

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate latestDate = today.plusDays(withinDays);

        return findAuthorizedInstruments().stream()
                .filter(fdr -> !isTerminal(fdr.getStatus()))
                .filter(fdr -> !fdr.getMaturityDate().isAfter(latestDate))
                .sorted(Comparator.comparing(
                        FdrInstrument::getMaturityDate))
                .map(fdr -> toMaturityResponse(fdr, today))
                .toList();
    }

    private MaturityRecordResponse toMaturityResponse(
            FdrInstrument fdr,
            LocalDate today) {

        long daysRemaining = ChronoUnit.DAYS.between(
                today, fdr.getMaturityDate());

        MaturityState maturityState;

        if (daysRemaining < 0) {
            maturityState = MaturityState.MATURED;
        } else if (daysRemaining <= expiringSoonDays) {
            maturityState = MaturityState.EXPIRING_SOON;
        } else {
            maturityState = MaturityState.UPCOMING;
        }

        return new MaturityRecordResponse(
                fdr.getId(),
                fdr.getInstitution().getId(),
                fdr.getInstitution().getInstitutionCode(),
                fdr.getInstitution().getName(),
                fdr.getInstrumentReference(),
                fdr.getBankName(),
                fdr.getDepositAmount(),
                fdr.getIssueDate(),
                fdr.getMaturityDate(),
                daysRemaining,
                maturityState,
                fdr.getStatus(),
                fdr.getVerificationStatus()
        );
    }

    private List<FdrInstrument> findAuthorizedInstruments() {
        Long institutionId = accessService.institutionScopeOrNull();
        return institutionId == null
                ? fdrRepository.findAll()
                : fdrRepository.findAllByInstitution_Id(institutionId);
    }

    private long countUnreadNotifications() {
        Long institutionId = accessService.institutionScopeOrNull();
        return institutionId == null
                ? notificationRepository.countByReadFalse()
                : notificationRepository.countByFdr_Institution_IdAndReadFalse(institutionId);
    }

    private boolean isVerificationPending(
            VerificationStatus status) {

        return status == VerificationStatus.PENDING
                || status == VerificationStatus.UNDER_REVIEW
                || status
                == VerificationStatus.EXTERNAL_CONFIRMATION_PENDING;
    }

    private boolean isTerminal(InstrumentStatus status) {
        return status == InstrumentStatus.RELEASED
                || status == InstrumentStatus.REJECTED
                || status == InstrumentStatus.RENEWED;
    }

    private void validateWindow(int days) {
        if (days < 0 || days > 3650) {
            throw new IllegalArgumentException(
                    "Window must be between 0 and 3650 days");
        }
    }
}