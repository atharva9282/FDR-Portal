package com.aicte.fdrportal.service;

import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.entity.Notification;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.NotificationType;
import com.aicte.fdrportal.enums.VerificationStatus;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;
import com.aicte.fdrportal.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Component
public class MaturityAlertScheduler {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final FdrInstrumentRepository fdrRepository;
    private final NotificationRepository notificationRepository;
    private final String reminderDaysCsv;

    public MaturityAlertScheduler(
            FdrInstrumentRepository fdrRepository,
            NotificationRepository notificationRepository,
            @Value("${app.monitoring.reminder-days:30,15,7,1}")
            String reminderDaysCsv) {

        this.fdrRepository = fdrRepository;
        this.notificationRepository = notificationRepository;
        this.reminderDaysCsv = reminderDaysCsv;
    }

    @Scheduled(
            cron = "${app.monitoring.cron:0 0 8 * * *}",
            zone = "Asia/Kolkata"
    )
    @Transactional
    public void scanAndCreateAlerts() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        List<Integer> reminderDays = parseReminderDays();

        for (FdrInstrument fdr : fdrRepository.findAll()) {

            if (isTerminal(fdr.getStatus())) {
                continue;
            }

            createMaturityAlertIfRequired(fdr, today, reminderDays);
            createVerificationAlertIfRequired(fdr);
        }
    }

    private void createMaturityAlertIfRequired(
            FdrInstrument fdr,
            LocalDate today,
            List<Integer> reminderDays) {

        LocalDate maturityDate = fdr.getMaturityDate();
        long daysRemaining =
                java.time.temporal.ChronoUnit.DAYS.between(
                        today, maturityDate);

        if (daysRemaining < 0) {
            createAlertIfMissing(
                    fdr,
                    NotificationType.MATURITY_OVERDUE,
                    "FDR-" + fdr.getId()
                            + "-MATURITY-OVERDUE-" + maturityDate,
                    "FDR maturity date has passed",
                    "The FDR maturity date was " + maturityDate
                            + ". Review the required renewal, replacement, "
                            + "or other applicable action.",
                    maturityDate
            );
            return;
        }

        // Find the closest configured reminder threshold that has
        // not yet been passed. This supports recovery if the system
        // was unavailable on the exact reminder day.
        Integer threshold = reminderDays.stream()
                .filter(day -> daysRemaining <= day)
                .findFirst()
                .orElse(null);

        if (threshold == null) {
            return;
        }

        createAlertIfMissing(
                fdr,
                NotificationType.MATURITY_REMINDER,
                "FDR-" + fdr.getId()
                        + "-MATURITY-" + threshold + "-" + maturityDate,
                "FDR maturity approaching",
                "The FDR matures on " + maturityDate
                        + ". Approximately " + daysRemaining
                        + " day(s) remain. Check whether renewal or "
                        + "replacement action is required.",
                maturityDate
        );
    }

    private void createVerificationAlertIfRequired(
            FdrInstrument fdr) {

        VerificationStatus status = fdr.getVerificationStatus();

        if (status == VerificationStatus.DISCREPANCY_FOUND) {
            createAlertIfMissing(
                    fdr,
                    NotificationType.DISCREPANCY_FOUND,
                    verificationAlertKey(fdr, "DISCREPANCY"),
                    "FDR discrepancy requires attention",
                    "A discrepancy has been recorded for this FDR. "
                            + "Review the latest verification remarks "
                            + "and take the required follow-up action.",
                    null
            );
            return;
        }

        if (status == VerificationStatus.PENDING
                || status == VerificationStatus.UNDER_REVIEW
                || status
                == VerificationStatus.EXTERNAL_CONFIRMATION_PENDING) {

            createAlertIfMissing(
                    fdr,
                    NotificationType.VERIFICATION_PENDING,
                    verificationAlertKey(fdr, status.name()),
                    "FDR verification is pending",
                    "Verification remains pending for this FDR. "
                            + "Review the case and record the result "
                            + "of the required scrutiny.",
                    null
            );
        }
    }

    private String verificationAlertKey(
            FdrInstrument fdr,
            String state) {

        long updatedAt = fdr.getUpdatedAt() == null
                ? 0L
                : fdr.getUpdatedAt().toEpochMilli();

        return "FDR-" + fdr.getId()
                + "-VERIFY-" + state
                + "-" + updatedAt;
    }

    private void createAlertIfMissing(
            FdrInstrument fdr,
            NotificationType type,
            String alertKey,
            String title,
            String message,
            LocalDate dueDate) {

        if (notificationRepository.existsByAlertKey(alertKey)) {
            return;
        }

        Notification notification = new Notification();
        notification.setFdr(fdr);
        notification.setType(type);
        notification.setAlertKey(alertKey);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setDueDate(dueDate);

        notificationRepository.save(notification);
    }

    private boolean isTerminal(InstrumentStatus status) {
        return status == InstrumentStatus.RELEASED
                || status == InstrumentStatus.REJECTED
                || status == InstrumentStatus.RENEWED;
    }

    private List<Integer> parseReminderDays() {
        return Arrays.stream(reminderDaysCsv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::parseInt)
                .filter(value -> value > 0)
                .distinct()
                .sorted()
                .toList();
    }
}