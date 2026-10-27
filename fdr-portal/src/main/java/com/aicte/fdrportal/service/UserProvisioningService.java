package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.UserCreateRequest;
import com.aicte.fdrportal.dto.UserProfileResponse;
import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.entity.UserAccount;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.exception.ConflictException;
import com.aicte.fdrportal.exception.ResourceNotFoundException;
import com.aicte.fdrportal.repository.InstitutionRepository;
import com.aicte.fdrportal.repository.UserAccountRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UserProvisioningService {

    private final UserAccountRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;

    public UserProvisioningService(
            UserAccountRepository userRepository,
            InstitutionRepository institutionRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public UserProfileResponse create(UserCreateRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 UTF-8 bytes");
        }

        if (request.role() == UserRole.ADMIN) {
            throw new IllegalArgumentException(
                    "Administrator accounts cannot be created through this endpoint");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(
                    "A user with this email already exists");
        }

        Institution institution = null;

        if (request.role() == UserRole.INSTITUTION) {

            if (request.institutionId() == null) {
                throw new IllegalArgumentException(
                        "Institution users must be linked to an institution");
            }

            institution = institutionRepository
                    .findById(request.institutionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Institution not found with ID: "
                                    + request.institutionId()));

            if (!institution.isActive()) {
                throw new IllegalArgumentException(
                        "Cannot create an account for an inactive institution");
            }

        } else if (request.institutionId() != null) {
            throw new IllegalArgumentException(
                    "Only institution users may be linked to an institution");
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setInstitution(institution);
        user.setEnabled(true);

        UserAccount saved = userRepository.save(user);
        auditEventService.recordSuccess(
                "USER_CREATED", "USER_ACCOUNT", saved.getId(),
                "role=" + saved.getRole()
                        + "; institutionId="
                        + (institution == null ? "none" : institution.getId()));
        return toProfile(saved);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse findByEmail(String email) {

        UserAccount user = userRepository
                .findWithInstitutionByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User account was not found"));

        return toProfile(user);
    }

    public UserProfileResponse toProfile(UserAccount user) {

        Institution institution = user.getInstitution();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                institution == null ? null : institution.getId(),
                institution == null ? null : institution.getName()
        );
    }
}