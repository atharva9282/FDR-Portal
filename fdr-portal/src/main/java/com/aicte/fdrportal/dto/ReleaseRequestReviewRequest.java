package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.ReleaseRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReleaseRequestReviewRequest(

        @NotNull
        ReleaseRequestStatus targetStatus,

        @NotBlank
        @Size(max = 2000)
        String remarks,

        @Size(max = 200)
        String completionReference
) {
}