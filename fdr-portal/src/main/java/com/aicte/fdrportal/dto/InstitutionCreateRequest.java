package com.aicte.fdrportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionCreateRequest(
        @NotBlank
        @Size(max = 60)
        String institutionCode,

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 200)
        String contactPerson,

        @Email
        @Size(max = 180)
        String contactEmail,

        @Size(max = 20)
        String contactPhone,

        @Size(max = 500)
        String address,

        @Size(max = 100)
        String district,

        @Size(max = 100)
        String state
) {
}
