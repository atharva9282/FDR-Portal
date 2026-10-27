package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.AuditEventResponse;
import com.aicte.fdrportal.entity.AuditEvent;
import com.aicte.fdrportal.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditEventService {

    private static final Instant EARLIEST_AUDIT_TIME =
            Instant.parse("0001-01-01T00:00:00Z");
    private static final Instant LATEST_AUDIT_TIME =
            Instant.parse("9999-12-31T23:59:59.999999Z");

    private final AuditEventRepository auditRepository;

    public AuditEventService(AuditEventRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Records a successful action in the caller's transaction so the audit
     * event is committed or rolled back with the business operation.
     */
    @Transactional
    public void recordSuccess(
            String action,
            String targetType,
            Object targetId,
            String details) {
        record(action, targetType, targetId, "SUCCESS", details);
    }

    /**
     * Records failed requests separately, so the audit event survives the
     * rollback of the rejected business operation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String action,
            String targetType,
            Object targetId,
            String details) {
        record(action, targetType, targetId, "FAILURE", details);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(
            Instant fromTime,
            Instant toTime,
            String actorEmail,
            String action,
            String targetType,
            Pageable pageable) {
        Instant effectiveFrom = fromTime == null
                ? EARLIEST_AUDIT_TIME
                : fromTime;
        Instant effectiveTo = toTime == null
                ? LATEST_AUDIT_TIME
                : toTime;

        return auditRepository.search(
                        effectiveFrom,
                        effectiveTo,
                        normalize(actorEmail),
                        normalize(action),
                        normalize(targetType),
                        pageable)
                .map(this::toResponse);
    }

    private void record(
            String action,
            String targetType,
            Object targetId,
            String outcome,
            String details) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String actorEmail = "anonymous";
        String actorRole = "ANONYMOUS";

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            actorEmail = safe(authentication.getName(), "unknown");
            actorRole = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring("ROLE_".length()))
                    .findFirst()
                    .orElse("AUTHENTICATED");
        }

        AuditEvent event = new AuditEvent();
        event.setActorEmail(limit(actorEmail, 180));
        event.setActorRole(limit(actorRole, 40));
        event.setAction(limit(safe(action, "UNKNOWN_ACTION"), 80));
        event.setTargetType(limit(safe(targetType, "UNKNOWN_TARGET"), 80));
        event.setTargetId(targetId == null ? null : limit(String.valueOf(targetId), 120));
        event.setOutcome(outcome);
        event.setDetails(details == null ? null : limit(details, 2000));
        auditRepository.save(event);
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getActorEmail(),
                event.getActorRole(),
                event.getAction(),
                event.getTargetType(),
                event.getTargetId(),
                event.getOutcome(),
                event.getDetails()
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
