package com.aicte.fdrportal.entity;

import com.aicte.fdrportal.enums.ReleaseRequestStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "release_refund_requests",
        indexes = {
                @Index(name = "idx_release_fdr", columnList = "fdr_id"),
                @Index(name = "idx_release_status", columnList = "status")
        }
)
public class ReleaseRefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fdr_id", nullable = false)
    private FdrInstrument fdr;

    @Column(name = "request_reason", nullable = false, length = 2000)
    private String requestReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReleaseRequestStatus status =
            ReleaseRequestStatus.SUBMITTED;

    @Column(name = "submitted_by", nullable = false, length = 180)
    private String submittedBy;

    @Column(name = "reviewed_by", length = 180)
    private String reviewedBy;

    @Column(length = 2000)
    private String remarks;

    @Column(name = "completion_reference", length = 200)
    private String completionReference;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ReleaseRefundRequest() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        requestedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public FdrInstrument getFdr() {
        return fdr;
    }

    public void setFdr(FdrInstrument fdr) {
        this.fdr = fdr;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    public ReleaseRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ReleaseRequestStatus status) {
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

    public String getCompletionReference() {
        return completionReference;
    }

    public void setCompletionReference(String completionReference) {
        this.completionReference = completionReference;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}