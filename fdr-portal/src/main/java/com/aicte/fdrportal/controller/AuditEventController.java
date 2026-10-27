package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.AuditEventResponse;
import com.aicte.fdrportal.service.AuditEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping
    public Page<AuditEventResponse> findAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @PageableDefault(size = 25, sort = "occurredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' date must be on or before 'to' date");
        }

        LocalDate earliestAllowedDate = LocalDate.of(2000, 1, 1);
        LocalDate latestAllowedDate = LocalDate.now(BUSINESS_ZONE).plusYears(1);
        if ((from != null && from.isBefore(earliestAllowedDate))
                || (to != null && to.isAfter(latestAllowedDate))) {
            throw new IllegalArgumentException(
                    "Audit date filters are outside the supported range");
        }
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException(
                    "Audit page size cannot exceed 100 records");
        }
        if (actorEmail != null && actorEmail.length() > 180) {
            throw new IllegalArgumentException("actorEmail filter cannot exceed 180 characters");
        }
        if (action != null && action.length() > 80) {
            throw new IllegalArgumentException("action filter cannot exceed 80 characters");
        }
        if (targetType != null && targetType.length() > 80) {
            throw new IllegalArgumentException("targetType filter cannot exceed 80 characters");
        }

        Instant fromTime = from == null
                ? null
                : from.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toTime = to == null
                ? null
                : to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant().minusNanos(1);

        return auditEventService.search(
                fromTime, toTime, actorEmail, action, targetType, pageable);
    }
}
