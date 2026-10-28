package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.ReleaseRequestCreateRequest;
import com.aicte.fdrportal.dto.ReleaseRequestResponse;
import com.aicte.fdrportal.dto.ReleaseRequestReviewRequest;
import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.entity.ReleaseRefundRequest;
import com.aicte.fdrportal.entity.UserAccount;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.ReleaseRequestStatus;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.enums.VerificationStatus;
import com.aicte.fdrportal.exception.ConflictException;
import com.aicte.fdrportal.exception.ResourceNotFoundException;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;
import com.aicte.fdrportal.repository.ReleaseRefundRequestRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReleaseRefundService {

    private final FdrInstrumentRepository fdrRepository;
    private final ReleaseRefundRequestRepository requestRepository;
    private final InstitutionAccessService accessService;
    private final AuditEventService auditEventService;

    public ReleaseRefundService(
            FdrInstrumentRepository fdrRepository,
            ReleaseRefundRequestRepository requestRepository,
            InstitutionAccessService accessService,
            AuditEventService auditEventService) {

        this.fdrRepository = fdrRepository;
        this.requestRepository = requestRepository;
        this.accessService = accessService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public ReleaseRequestResponse submit(
            Long fdrId,
            ReleaseRequestCreateRequest request) {

        UserAccount actor = accessService.currentUser();

        if (actor.getRole() != UserRole.INSTITUTION
                && actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException(
                    "Only an institution or administrator may submit this request");
        }

        FdrInstrument fdr = fdrRepository.findById(fdrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FDR not found with ID: " + fdrId));

        accessService.assertCanAccessFdr(fdr);

        if (fdr.getStatus() != InstrumentStatus.ACTIVE) {
            throw new ConflictException(
                    "Only an active FDR can enter the release/refund workflow");
        }

        boolean verified =
                fdr.getVerificationStatus()
                        == VerificationStatus.VERIFIED_BY_OFFICIAL
                        || fdr.getVerificationStatus()
                        == VerificationStatus.EXTERNALLY_CONFIRMED;

        if (!verified) {
            throw new ConflictException(
                    "The FDR must be verified before a release/refund request");
        }

        boolean existingRequest =
                requestRepository.existsByFdr_IdAndStatusIn(
                        fdrId,
                        List.of(
                                ReleaseRequestStatus.SUBMITTED,
                                ReleaseRequestStatus.UNDER_REVIEW,
                                ReleaseRequestStatus.APPROVED
                        ));

        if (existingRequest) {
            throw new ConflictException(
                    "An unresolved release/refund request already exists");
        }

        ReleaseRefundRequest entity = new ReleaseRefundRequest();
        entity.setFdr(fdr);
        entity.setRequestReason(request.reason().trim());
        entity.setStatus(ReleaseRequestStatus.SUBMITTED);
        entity.setSubmittedBy(actor.getEmail());

        fdr.setStatus(InstrumentStatus.RELEASE_REQUESTED);
        fdrRepository.save(fdr);

        ReleaseRefundRequest saved = requestRepository.save(entity);
        auditEventService.recordSuccess(
                "RELEASE_REQUEST_SUBMITTED", "RELEASE_REQUEST", saved.getId(),
                "fdrId=" + fdr.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReleaseRequestResponse> findByFdrId(Long fdrId) {

        FdrInstrument fdr = fdrRepository.findById(fdrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FDR not found with ID: " + fdrId));

        UserAccount actor = accessService.currentUser();
        requireRole(actor, UserRole.ADMIN, UserRole.INSTITUTION,
                UserRole.REGULATION_OFFICIAL);
        accessService.assertCanAccessFdr(fdr);

        return requestRepository
                .findByFdr_IdOrderByRequestedAtDesc(fdrId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReleaseRequestResponse> findAllForApprovalQueue() {
        UserAccount actor = accessService.currentUser();
        requireRole(actor, UserRole.ADMIN, UserRole.APPROVING_AUTHORITY);
        return requestRepository.findAllByOrderByRequestedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReleaseRequestResponse review(
            Long requestId,
            ReleaseRequestReviewRequest reviewRequest) {

        ReleaseRefundRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Release/refund request not found: " + requestId));

        UserAccount actor = accessService.currentUser();

        accessService.assertCanAccessFdr(request.getFdr());

        ReleaseRequestStatus target = reviewRequest.targetStatus();

        switch (target) {
            case SUBMITTED ->
                    throw new IllegalArgumentException(
                            "A review cannot reset a request to SUBMITTED");

            case UNDER_REVIEW -> {
                requireRole(
                        actor,
                        UserRole.ADMIN,
                        UserRole.APPROVING_AUTHORITY
                );

                requireCurrentStatus(
                        request,
                        ReleaseRequestStatus.SUBMITTED
                );
            }

            case APPROVED, REJECTED -> {
                requireRole(
                        actor,
                        UserRole.ADMIN,
                        UserRole.APPROVING_AUTHORITY
                );

                requireCurrentStatus(
                        request,
                        ReleaseRequestStatus.UNDER_REVIEW
                );

                if (target == ReleaseRequestStatus.APPROVED
                        && request.getFdr().getStatus()
                        != InstrumentStatus.RELEASE_REQUESTED) {
                    throw new ConflictException(
                            "FDR is not in the expected release-requested state");
                }
            }

            case COMPLETED -> {
                requireRole(
                        actor,
                        UserRole.ADMIN,
                        UserRole.APPROVING_AUTHORITY
                );

                requireCurrentStatus(
                        request,
                        ReleaseRequestStatus.APPROVED
                );

                if (reviewRequest.completionReference() == null
                        || reviewRequest.completionReference().isBlank()) {
                    throw new IllegalArgumentException(
                            "A completion reference is required to record release");
                }

                if (request.getFdr().getStatus()
                        != InstrumentStatus.RELEASE_REQUESTED) {
                    throw new ConflictException(
                            "FDR is not in the expected release-requested state");
                }
            }
        }

        request.setStatus(target);
        request.setReviewedBy(actor.getEmail());
        request.setRemarks(reviewRequest.remarks().trim());

        if (target == ReleaseRequestStatus.REJECTED) {
            request.getFdr().setStatus(InstrumentStatus.ACTIVE);
            fdrRepository.save(request.getFdr());
        }

        if (target == ReleaseRequestStatus.COMPLETED) {
            request.setCompletionReference(
                    reviewRequest.completionReference().trim());

            request.getFdr().setStatus(InstrumentStatus.RELEASED);
            fdrRepository.save(request.getFdr());
        }

        ReleaseRefundRequest saved = requestRepository.save(request);
        auditEventService.recordSuccess(
                "RELEASE_REQUEST_REVIEWED", "RELEASE_REQUEST", saved.getId(),
                "fdrId=" + saved.getFdr().getId()
                        + "; status=" + saved.getStatus()
                        + "; completionReferencePresent="
                        + (saved.getCompletionReference() != null));
        return toResponse(saved);
    }

    private void requireRole(
            UserAccount actor,
            UserRole... roles) {

        for (UserRole role : roles) {
            if (actor.getRole() == role) {
                return;
            }
        }

        throw new AccessDeniedException(
                "You are not authorized to perform this action");
    }

    private void requireCurrentStatus(
            ReleaseRefundRequest request,
            ReleaseRequestStatus expected) {

        if (request.getStatus() != expected) {
            throw new ConflictException(
                    "Request must be in " + expected
                            + " status before this action");
        }
    }

    private ReleaseRequestResponse toResponse(
            ReleaseRefundRequest request) {

        FdrInstrument fdr = request.getFdr();
        Institution institution = fdr.getInstitution();

        return new ReleaseRequestResponse(
                request.getId(),
                fdr.getId(),
                institution.getId(),
                institution.getName(),
                fdr.getInstrumentReference(),
                fdr.getDepositAmount(),
                fdr.getMaturityDate(),
                request.getStatus(),
                request.getRequestReason(),
                request.getSubmittedBy(),
                request.getReviewedBy(),
                request.getRemarks(),
                request.getCompletionReference(),
                request.getRequestedAt(),
                request.getUpdatedAt()
        );
    }
}