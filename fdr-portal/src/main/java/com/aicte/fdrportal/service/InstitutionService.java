package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.InstitutionCreateRequest;
import com.aicte.fdrportal.dto.InstitutionResponse;
import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.exception.ConflictException;
import com.aicte.fdrportal.exception.ResourceNotFoundException;
import com.aicte.fdrportal.repository.InstitutionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public InstitutionService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @Transactional
    public InstitutionResponse create(InstitutionCreateRequest request)
    {
        if(institutionRepository.existsByInstitutionCode(request.institutionCode()))
        {
            throw new ConflictException("Institution code already exists");
        }

        Institution institution = new Institution();
        institution.setInstitutionCode(request.institutionCode().trim());
        institution.setName(request.name().trim());
        institution.setContactPerson(request.contactPerson());
        institution.setContactEmail(request.contactEmail());
        institution.setContactPhone(request.contactPhone());
        institution.setAddress(request.address());
        institution.setDistrict(request.district());
        institution.setState(request.state());

        return toResponse(institutionRepository.save(institution));

    }

    @Transactional(readOnly = true)
    public Page<InstitutionResponse> findAll(Pageable pageable)
    {
        return institutionRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InstitutionResponse findById(Long id) {

        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Institution not found with ID: " + id));

        return toResponse(institution);
    }


    private InstitutionResponse toResponse(Institution institution) {

        return new InstitutionResponse(
                institution.getId(),
                institution.getInstitutionCode(),
                institution.getName(),
                institution.getContactPerson(),
                institution.getContactEmail(),
                institution.getContactPhone(),
                institution.getAddress(),
                institution.getDistrict(),
                institution.getState(),
                institution.isActive(),
                institution.getCreatedAt(),
                institution.getUpdatedAt()
        );
    }
}
