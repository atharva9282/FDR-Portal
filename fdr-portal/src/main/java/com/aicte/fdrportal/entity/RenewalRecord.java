package com.aicte.fdrportal.entity;

import com.aicte.fdrportal.enums.RenewalRequestStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "renewal_records",
        indexes = {
                @Index(name = "idx_renewal_original", columnList = "original_fdr_id"),
                @Index(name = "idx_renewal_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_renewal_replacement",
                        columnNames = "replacement_fdr_id"
                )
        }
)
public class RenewalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_fdr_id", nullable = false)
    private FdrInstrument originalFdr;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "replacement_fdr_id", nullable = false)
    private FdrInstrument replacementFdr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RenewalRequestStatus status = RenewalRequestStatus.SUBMITTED;

    @Column(name = "submitted_by", nullable = false, length = 180)
    private String submittedBy;

    @Column(name = "reviewed_by", length = 180)
    private String reviewedBy;

    @Column(length = 2000)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RenewalRecord() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public FdrInstrument getOriginalFdr() {
        return originalFdr;
    }

    public void setOriginalFdr(FdrInstrument originalFdr) {
        this.originalFdr = originalFdr;
    }

    public FdrInstrument getReplacementFdr() {
        return replacementFdr;
    }

    public void setReplacementFdr(FdrInstrument replacementFdr) {
        this.replacementFdr = replacementFdr;
    }

    public RenewalRequestStatus getStatus() {
        return status;
    }

    public void setStatus(RenewalRequestStatus status) {
        this.status = status;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}