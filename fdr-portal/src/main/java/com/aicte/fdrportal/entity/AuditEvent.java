package com.aicte.fdrportal.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_audit_actor_email", columnList = "actor_email"),
                @Index(name = "idx_audit_action", columnList = "action")
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", nullable = false, length = 180)
    private String actorEmail;

    @Column(name = "actor_role", nullable = false, length = 40)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;

    @Column(name = "target_id", length = 120)
    private String targetId;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public AuditEvent() {
    }

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
