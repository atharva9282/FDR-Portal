package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.RenewalCreateRequest;
import com.aicte.fdrportal.dto.RenewalResponse;
import com.aicte.fdrportal.dto.RenewalReviewRequest;
import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.entity.RenewalRecord;
import com.aicte.fdrportal.entity.UserAccount;
import com.aicte.fdrportal.enums.DocumentType;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.RenewalRequestStatus;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.enums.VerificationStatus;
import com.aicte.fdrportal.exception.ConflictException;
import com.aicte.fdrportal.exception.ResourceNotFoundException;
import com.aicte.fdrportal.repository.FdrDocumentRepository;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;
import com.aicte.fdrportal.repository.RenewalRecordRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RenewalService {

    private final FdrInstrumentRepository fdrRepository;
    private final RenewalRecordRepository renewalRepository;
    private final FdrDocumentRepository documentRepository;
    private final InstitutionAccessService accessService;
    private final AuditEventService auditEventService;

    public RenewalService(
            FdrInstrumentRepository fdrRepository,
            RenewalRecordRepository renewalRepository,
            FdrDocumentRepository documentRepository,
            InstitutionAccessService accessService,
            AuditEventService auditEventService) {

        this.fdrRepository = fdrRepository;
        this.renewalRepository = renewalRepository;
        this.documentRepository = documentRepository;
        this.accessService = accessService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public RenewalResponse create(
            Long originalFdrId,
            RenewalCreateRequest request) {

        UserAccount actor = accessService.currentUser();
        if (actor.getRole() != UserRole.INSTITUTION
                && actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException(
                    "Only an institution user or administrator may submit a renewal request");
        }

        FdrInstrument original = fdrRepository.findById(originalFdrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Original FDR not found: " + originalFdrId));

        accessService.assertCanAccessFdr(original);

        if (request.maturityDate().isBefore(
                LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")))) {
            throw new IllegalArgumentException(
                    "Replacement FDR maturity date must be in the future");
        }

        if (!request.maturityDate().isAfter(request.issueDate())) {
            throw new IllegalArgumentException(
                    "Maturity date must be after issue date");
        }

        if (original.getStatus() != InstrumentStatus.ACTIVE) {
            throw new ConflictException(
                    "Only an active FDR can enter the renewal workflow");
        }

        boolean alreadyVerified =
                original.getVerificationStatus()
                        == VerificationStatus.VERIFIED_BY_OFFICIAL
                        || original.getVerificationStatus()
                        == VerificationStatus.EXTERNALLY_CONFIRMED;

        if (!alreadyVerified) {
            throw new ConflictException(
                    "The original FDR must be verified before renewal processing");
        }

        boolean pendingRenewal =
                renewalRepository.existsByOriginalFdr_IdAndStatusIn(
                        originalFdrId,
                        List.of(
                                RenewalRequestStatus.SUBMITTED,
                                RenewalRequestStatus.UNDER_REVIEW
                        ));

        if (pendingRenewal) {
            throw new ConflictException(
                    "This FDR already has a pending renewal request");
        }

        String reference = request.instrumentReference().trim();
        String bank = request.bankName().trim();

        boolean duplicate =
                fdrRepository
                        .existsByBankNameIgnoreCaseAndInstrumentReferenceIgnoreCase(
                                bank, reference);

        if (duplicate) {
            throw new ConflictException(
                    "An FDR with this bank and instrument reference already exists");
        }

        Institution institution = original.getInstitution();

        FdrInstrument replacement = new FdrInstrument();
        replacement.setInstitution(institution);
        replacement.setInstrumentReference(reference);
        replacement.setBankName(bank);
        replacement.setBranchName(request.branchName());
        replacement.setDepositAmount(request.depositAmount());
        replacement.setIssueDate(request.issueDate());
        replacement.setMaturityDate(request.maturityDate());
        replacement.setStatus(InstrumentStatus.SUBMITTED);
        replacement.setVerificationStatus(VerificationStatus.PENDING);

        replacement = fdrRepository.save(replacement);

        RenewalRecord record = new RenewalRecord();
        record.setOriginalFdr(original);
        record.setReplacementFdr(replacement);
        record.setStatus(RenewalRequestStatus.SUBMITTED);
        record.setSubmittedBy(actor.getEmail());
        record.setRemarks(normalize(request.remarks()));

        original.setStatus(InstrumentStatus.RENEWAL_PENDING);
        fdrRepository.save(original);

        RenewalRecord saved = renewalRepository.save(record);
        auditEventService.recordSuccess(
                "RENEWAL_SUBMITTED", "RENEWAL", saved.getId(),
                "originalFdrId=" + original.getId()
                        + "; replacementFdrId=" + replacement.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RenewalResponse> findAllForApprovalQueue() {
        UserAccount actor = accessService.currentUser();
        requireRole(actor, UserRole.ADMIN, UserRole.APPROVING_AUTHORITY);
        return renewalRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RenewalResponse> findByOriginalFdrId(Long originalFdrId) {

        FdrInstrument original = fdrRepository.findById(originalFdrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Original FDR not found: " + originalFdrId));

        UserAccount actor = accessService.currentUser();
        requireRole(actor, UserRole.ADMIN, UserRole.INSTITUTION,
                UserRole.APPROVING_AUTHORITY, UserRole.REGULATION_OFFICIAL);
        accessService.assertCanAccessFdr(original);

        return renewalRepository
                .findByOriginalFdr_IdOrderByCreatedAtDesc(originalFdrId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RenewalResponse review(
            Long renewalId,
            RenewalReviewRequest request) {

        RenewalRecord record = renewalRepository.findById(renewalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Renewal record not found: " + renewalId));

        UserAccount actor = accessService.currentUser();

        accessService.assertCanAccessFdr(record.getOriginalFdr());

        RenewalRequestStatus target = request.targetStatus();

        if (target == RenewalRequestStatus.SUBMITTED) {
            throw new IllegalArgumentException(
                    "A review cannot reset a renewal to SUBMITTED");
        }

        if (target == RenewalRequestStatus.UNDER_REVIEW) {

            requireRole(
                    actor,
                    UserRole.ADMIN,
                    UserRole.APPROVING_AUTHORITY
            );

            if (record.getStatus() != RenewalRequestStatus.SUBMITTED) {
                throw new ConflictException(
                        "Only a submitted renewal can move to UNDER_REVIEW");
            }

        } else {

            requireRole(
                    actor,
                    UserRole.ADMIN,
                    UserRole.APPROVING_AUTHORITY
            );

            if (record.getStatus() != RenewalRequestStatus.UNDER_REVIEW) {
                throw new ConflictException(
                        "The renewal must be under review before a final decision");
            }
        }

        // Validate the replacement document before mutating the managed request.
        // This keeps the record unchanged when approval must be rejected.
        if (target == RenewalRequestStatus.APPROVED) {
            boolean documentUploaded =
                    documentRepository.existsByFdr_IdAndDocumentType(
                            record.getReplacementFdr().getId(),
                            DocumentType.RENEWAL_REPLACEMENT);

            if (!documentUploaded) {
                throw new ConflictException(
                        "Upload the replacement FDR document before approving renewal");
            }
        }

        record.setStatus(target);
        record.setReviewedBy(actor.getEmail());
        record.setRemarks(request.remarks().trim());

        FdrInstrument original = record.getOriginalFdr();
        FdrInstrument replacement = record.getReplacementFdr();

        if (target == RenewalRequestStatus.APPROVED) {

            original.setStatus(InstrumentStatus.RENEWED);

            // Approval of the renewal request does not itself verify
            // the authenticity of the new financial instrument.
            replacement.setStatus(InstrumentStatus.UNDER_REVIEW);
            replacement.setVerificationStatus(
                    VerificationStatus.UNDER_REVIEW);
            replacement.setVerificationRemarks(
                    "Replacement submitted; instrument verification is required.");

            fdrRepository.save(original);
            fdrRepository.save(replacement);

        } else if (target == RenewalRequestStatus.REJECTED) {

            original.setStatus(InstrumentStatus.ACTIVE);
            replacement.setStatus(InstrumentStatus.REJECTED);

            fdrRepository.save(original);
            fdrRepository.save(replacement);
        }

        RenewalRecord saved = renewalRepository.save(record);
        auditEventService.recordSuccess(
                "RENEWAL_REVIEWED", "RENEWAL", saved.getId(),
                "status=" + saved.getStatus());
        return toResponse(saved);
    }

    private void requireRole(
            UserAccount user,
            UserRole... allowedRoles) {

        for (UserRole role : allowedRoles) {
            if (user.getRole() == role) {
                return;
            }
        }

        throw new AccessDeniedException(
                "You are not authorized to perform this renewal action");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private RenewalResponse toResponse(RenewalRecord record) {

        return new RenewalResponse(
                record.getId(),
                record.getOriginalFdr().getId(),
                record.getOriginalFdr().getInstrumentReference(),
                record.getReplacementFdr().getId(),
                record.getReplacementFdr().getInstrumentReference(),
                record.getOriginalFdr().getInstitution().getName(),
                record.getOriginalFdr().getDepositAmount(),
                record.getOriginalFdr().getMaturityDate(),
                record.getReplacementFdr().getBankName(),
                record.getReplacementFdr().getBranchName(),
                record.getReplacementFdr().getDepositAmount(),
                record.getReplacementFdr().getIssueDate(),
                record.getReplacementFdr().getMaturityDate(),
                record.getStatus(),
                record.getSubmittedBy(),
                record.getReviewedBy(),
                record.getRemarks(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}