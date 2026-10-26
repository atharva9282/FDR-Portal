package com.aicte.fdrportal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RenewalCreateRequest(

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
        LocalDate maturityDate,

        @Size(max = 2000)
        String remarks
) {
}