package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.RenewalRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RenewalReviewRequest(

        @NotNull
        RenewalRequestStatus targetStatus,

        @NotBlank
        @Size(max = 2000)
        String remarks
) {
}