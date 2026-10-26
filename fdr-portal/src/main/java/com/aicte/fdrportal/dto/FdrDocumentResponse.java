package com.aicte.fdrportal.dto;

import com.aicte.fdrportal.enums.DocumentType;

import java.time.Instant;

public record FdrDocumentResponse(
        Long id,
        Long fdrId,
        DocumentType documentType,
        String originalFilename,
        String mediaType,
        long sizeBytes,
        String sha256,
        Instant uploadedAt
) {
}
