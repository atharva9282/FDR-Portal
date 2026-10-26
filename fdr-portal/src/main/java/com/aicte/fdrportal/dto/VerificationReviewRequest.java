package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.VerificationMethod;
import com.aicte.fdrportal.enums.VerificationOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerificationReviewRequest(

        @NotNull
        VerificationOutcome outcome,

        @NotNull
        VerificationMethod verificationMethod,

        @Size(max = 200)
        String sourceReference,

        @NotBlank
        @Size(max = 2000)
        String remarks
) {
}