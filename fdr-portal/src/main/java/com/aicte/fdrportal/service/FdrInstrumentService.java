package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.FdrCreateRequest;
import com.aicte.fdrportal.dto.FdrResponse;
import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.exception.ConflictException;
import com.aicte.fdrportal.exception.ResourceNotFoundException;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;
import com.aicte.fdrportal.repository.InstitutionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FdrInstrumentService {

    private final FdrInstrumentRepository fdrRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionAccessService institutionAccessService;
    private final AuditEventService auditEventService;

    public FdrInstrumentService(
            FdrInstrumentRepository fdrRepository,
            InstitutionRepository institutionRepository,
            InstitutionAccessService institutionAccessService,
            AuditEventService auditEventService) {
        this.fdrRepository = fdrRepository;
        this.institutionRepository = institutionRepository;
        this.institutionAccessService = institutionAccessService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public FdrResponse create(FdrCreateRequest request) {

        var actor = institutionAccessService.currentUser();
        if (actor.getRole() != UserRole.ADMIN
                && actor.getRole() != UserRole.INSTITUTION) {
            throw new AccessDeniedException(
                    "Only an institution user or administrator may register an FDR");
        }

        if (!request.maturityDate().isAfter(request.issueDate())) {
            throw new IllegalArgumentException(
                    "Maturity date must be after issue date");
        }

        Institution institution = institutionRepository
                .findById(request.institutionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Institution not found with ID: "
                                + request.institutionId()));

        if (!institution.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot submit an FDR for an inactive institution");
        }

        boolean duplicate =
                fdrRepository
                        .existsByBankNameIgnoreCaseAndInstrumentReferenceIgnoreCase(
                                request.bankName().trim(),
                                request.instrumentReference().trim()
                        );

        if (duplicate) {
            throw new ConflictException("An FDR with this bank and instrument reference already exists");
        }
        institutionAccessService.assertCanUseInstitution(
                institution.getId());

        FdrInstrument fdr = new FdrInstrument();
        fdr.setInstitution(institution);
        fdr.setInstrumentReference(
                request.instrumentReference().trim());
        fdr.setBankName(request.bankName().trim());
        fdr.setBranchName(request.branchName());
        fdr.setDepositAmount(request.depositAmount());
        fdr.setIssueDate(request.issueDate());
        fdr.setMaturityDate(request.maturityDate());

        // Initial workflow statuses come from the entity defaults.
        // Client requests cannot directly set verification outcomes.
        fdr.setStatus(InstrumentStatus.SUBMITTED);

        FdrInstrument saved = fdrRepository.save(fdr);
        auditEventService.recordSuccess(
                "FDR_CREATED", "FDR", saved.getId(),
                "institutionId=" + saved.getInstitution().getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<FdrResponse> findAll( Long institutionId, InstrumentStatus status, Pageable pageable)
    {

        Page<FdrInstrument> results;

        Long scopedInstitutionId =
                institutionAccessService.institutionScopeOrNull();

        if (scopedInstitutionId != null) {

            if (institutionId != null
                    && !institutionId.equals(scopedInstitutionId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You cannot query another institution's FDRs");
            }

            institutionId = scopedInstitutionId;
        }

        if (institutionId != null && status != null) {
            results = fdrRepository.findByInstitution_IdAndStatus(
                    institutionId, status, pageable);
        } else if (institutionId != null) {
            results = fdrRepository.findByInstitution_Id(
                    institutionId, pageable);
        } else if (status != null) {
            results = fdrRepository.findByStatus(status, pageable);
        } else {
            results = fdrRepository.findAll(pageable);
        }

        return results.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FdrResponse findById(Long id) {

        FdrInstrument fdr = fdrRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FDR not found with ID: " + id));

        institutionAccessService.assertCanAccessFdr(fdr);

        return toResponse(fdr);
    }




    private FdrResponse toResponse(FdrInstrument fdr) {

        Institution institution = fdr.getInstitution();

        return new FdrResponse(
                fdr.getId(),
                institution.getId(),
                institution.getInstitutionCode(),
                institution.getName(),
                fdr.getInstrumentReference(),
                fdr.getBankName(),
                fdr.getBranchName(),
                fdr.getDepositAmount(),
                fdr.getIssueDate(),
                fdr.getMaturityDate(),
                fdr.getStatus(),
                fdr.getVerificationStatus(),
                fdr.getVerificationRemarks(),
                fdr.getSubmittedAt(),
                fdr.getCreatedAt(),
                fdr.getUpdatedAt()
        );
    }

}
