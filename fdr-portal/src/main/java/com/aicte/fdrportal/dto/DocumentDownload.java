package com.aicte.fdrportal.dto;

import org.springframework.core.io.Resource;

public record DocumentDownload(
        Resource resource,
        String originalFilename,
        String mediaType,
        long sizeBytes
) {
}
