package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.UserRole;

public record UserProfileResponse(
        Long id,
        String email,
        UserRole role,
        Long institutionId,
        String institutionName
) {
}