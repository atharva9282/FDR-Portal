package com.aicte.fdrportal.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FdrCreateRequest(
        @NotNull
        @Positive
        Long institutionId,

        @NotBlank
        @Size(max = 120)
        String instrumentReference,

        @NotBlank
        @Size(max = 150)
        String bankName,

        @Size(max = 150)
        String branchName,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal depositAmount,

        @NotNull
        LocalDate issueDate,

        @NotNull
        LocalDate maturityDate
) {
}
