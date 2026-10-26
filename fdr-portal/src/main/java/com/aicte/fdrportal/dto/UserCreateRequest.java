package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Email String email,

        @NotBlank
        @Size(min = 12, max = 72)
        String password,

        @NotNull
        UserRole role,

        Long institutionId
) {
}