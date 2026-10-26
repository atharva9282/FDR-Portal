package com.aicte.fdrportal.entity;

import com.aicte.fdrportal.enums.VerificationMethod;
import com.aicte.fdrportal.enums.VerificationOutcome;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "verification_reviews",
        indexes = {
                @Index(
                        name = "idx_review_fdr",
                        columnList = "fdr_id"
                ),
                @Index(
                        name = "idx_review_time",
                        columnList = "reviewed_at"
                )
        }
)
public class VerificationReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fdr_id", nullable = false)
    private FdrInstrument fdr;

    @Column(name = "reviewer_name", nullable = false, length = 150)
    private String reviewerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 50)
    private VerificationOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false, length = 50)
    private VerificationMethod verificationMethod;

    @Column(name = "source_reference", length = 200)
    private String sourceReference;

    @Column(name = "remarks", nullable = false, length = 2000)
    private String remarks;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt;

    public VerificationReview() {
    }

    @PrePersist
    protected void onCreate() {
        reviewedAt = Instant.now();
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

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public VerificationOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(VerificationOutcome outcome) {
        this.outcome = outcome;
    }

    public VerificationMethod getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(
            VerificationMethod verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}