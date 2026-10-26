package com.aicte.fdrportal.dto;

import java.time.Instant;

public record InstitutionResponse(
        Long id,
        String institutionCode,
        String name,
        String contactPerson,
        String contactEmail,
        String contactPhone,
        String address,
        String district,
        String state,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
