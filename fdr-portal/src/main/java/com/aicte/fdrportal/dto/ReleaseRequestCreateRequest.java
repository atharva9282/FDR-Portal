package com.aicte.fdrportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseRequestCreateRequest(

        @NotBlank
        @Size(max = 2000)
        String reason
) {
}