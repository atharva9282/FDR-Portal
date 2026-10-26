package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.VerificationReviewRequest;
import com.aicte.fdrportal.dto.VerificationReviewResponse;
import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.entity.VerificationReview;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.VerificationMethod;
import com.aicte.fdrportal.enums.VerificationOutcome;
import com.aicte.fdrportal.enums.VerificationStatus;
import com.aicte.fdrportal.exception.ResourceNotFoundException;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;
import com.aicte.fdrportal.repository.VerificationReviewRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VerificationReviewService {

    private final FdrInstrumentRepository fdrRepository;
    private final VerificationReviewRepository reviewRepository;
    private final InstitutionAccessService institutionAccessService;

    public VerificationReviewService(
            FdrInstrumentRepository fdrRepository,
            VerificationReviewRepository reviewRepository, InstitutionAccessService institutionAccessService) {
        this.fdrRepository = fdrRepository;
        this.reviewRepository = reviewRepository;
        this.institutionAccessService = institutionAccessService;
    }

    @Transactional
    public VerificationReviewResponse create(
            Long fdrId,
            VerificationReviewRequest request) {

        FdrInstrument fdr = fdrRepository.findById(fdrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FDR not found with ID: " + fdrId));

        validateReview(request);

        VerificationReview review = new VerificationReview();
        review.setFdr(fdr);
        review.setReviewerName(
                institutionAccessService.currentUser().getEmail());
        review.setOutcome(request.outcome());
        review.setVerificationMethod(request.verificationMethod());
        review.setSourceReference(
                normalize(request.sourceReference()));
        review.setRemarks(request.remarks().trim());

        applyOutcomeToFdr(fdr, request.outcome());

        // Both changes are saved in the same transaction.
        fdrRepository.save(fdr);

        VerificationReview savedReview =
                reviewRepository.save(review);

        return toResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public List<VerificationReviewResponse> findByFdrId(
            Long fdrId) {

        if (!fdrRepository.existsById(fdrId)) {
            throw new ResourceNotFoundException(
                    "FDR not found with ID: " + fdrId);
        }

        return reviewRepository
                .findByFdr_IdOrderByReviewedAtDesc(fdrId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateReview(
            VerificationReviewRequest request) {

        if (request.outcome()
                == VerificationOutcome.EXTERNALLY_CONFIRMED) {

            boolean authorizedExternalMethod =
                    request.verificationMethod()
                            == VerificationMethod.BANK_ISSUED_CONFIRMATION
                            || request.verificationMethod()
                            == VerificationMethod.OTHER_AUTHORIZED_SOURCE;

            if (!authorizedExternalMethod) {
                throw new IllegalArgumentException(
                        "External confirmation requires a bank-issued "
                                + "confirmation or another authorized external source");
            }

            if (request.sourceReference() == null
                    || request.sourceReference().isBlank()) {
                throw new IllegalArgumentException(
                        "A reference to the external confirmation is required");
            }
        }

        if (request.outcome()
                == VerificationOutcome.VERIFIED_BY_OFFICIAL
                && request.verificationMethod()
                != VerificationMethod.OFFICIAL_DOCUMENT_REVIEW) {

            throw new IllegalArgumentException(
                    "Official document verification must use "
                            + "OFFICIAL_DOCUMENT_REVIEW");
        }
    }

    private void applyOutcomeToFdr(
            FdrInstrument fdr,
            VerificationOutcome outcome) {

        switch (outcome) {

            case UNDER_REVIEW -> {
                fdr.setVerificationStatus(
                        VerificationStatus.UNDER_REVIEW);
                fdr.setStatus(InstrumentStatus.UNDER_REVIEW);
            }

            case DISCREPANCY_FOUND -> {
                fdr.setVerificationStatus(
                        VerificationStatus.DISCREPANCY_FOUND);
                fdr.setStatus(InstrumentStatus.ACTION_REQUIRED);
            }

            case VERIFIED_BY_OFFICIAL -> {
                fdr.setVerificationStatus(
                        VerificationStatus.VERIFIED_BY_OFFICIAL);
                fdr.setStatus(InstrumentStatus.ACTIVE);
            }

            case EXTERNAL_CONFIRMATION_PENDING -> {
                fdr.setVerificationStatus(
                        VerificationStatus.EXTERNAL_CONFIRMATION_PENDING);
                fdr.setStatus(InstrumentStatus.UNDER_REVIEW);
            }

            case EXTERNALLY_CONFIRMED -> {
                fdr.setVerificationStatus(
                        VerificationStatus.EXTERNALLY_CONFIRMED);
                fdr.setStatus(InstrumentStatus.ACTIVE);
            }
        }

        fdr.setVerificationRemarks(
                outcome.name() + ": " + outcomeDescription(outcome)
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private VerificationReviewResponse toResponse(
            VerificationReview review) {

        return new VerificationReviewResponse(
                review.getId(),
                review.getFdr().getId(),
                review.getReviewerName(),
                review.getOutcome(),
                review.getVerificationMethod(),
                review.getSourceReference(),
                review.getRemarks(),
                review.getReviewedAt()
        );
    }

    private String outcomeDescription(VerificationOutcome outcome) {
        return switch (outcome) {
            case UNDER_REVIEW ->
                    "Verification is in progress";
            case DISCREPANCY_FOUND ->
                    "Discrepancy requires further review";
            case VERIFIED_BY_OFFICIAL ->
                    "Official document review completed";
            case EXTERNAL_CONFIRMATION_PENDING ->
                    "Independent confirmation is pending";
            case EXTERNALLY_CONFIRMED ->
                    "Confirmation recorded from an authorized external source";
        };
    }
}