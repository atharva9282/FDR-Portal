package com.aicte.fdrportal.service;

import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.entity.UserAccount;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.repository.UserAccountRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class InstitutionAccessService {

    private final UserAccountRepository userRepository;

    public InstitutionAccessService(
            UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount currentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException(
                    "Authentication is required");
        }

        return userRepository
                .findWithInstitutionByEmailIgnoreCase(
                        authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user account was not found"));
    }

    /**
     * Returns the institution ID for an institution account.
     * Returns null for other roles.
     */
    public Long institutionScopeOrNull() {

        UserAccount user = currentUser();

        if (user.getRole() != UserRole.INSTITUTION) {
            return null;
        }

        if (user.getInstitution() == null) {
            throw new AccessDeniedException(
                    "Institution account is not linked to an institution");
        }

        return user.getInstitution().getId();
    }

    public void assertCanUseInstitution(Long institutionId) {

        UserAccount user = currentUser();

        if (user.getRole() == UserRole.INSTITUTION) {

            if (user.getInstitution() == null
                    || !user.getInstitution().getId().equals(institutionId)) {
                throw new AccessDeniedException(
                        "You cannot access another institution's records");
            }
        }
    }

    public void assertCanAccessFdr(FdrInstrument fdr) {

        UserAccount user = currentUser();

        if (user.getRole() == UserRole.INSTITUTION) {

            if (user.getInstitution() == null
                    || !user.getInstitution().getId()
                    .equals(fdr.getInstitution().getId())) {
                throw new AccessDeniedException(
                        "You cannot access this institution's FDR");
            }
        }
    }
}