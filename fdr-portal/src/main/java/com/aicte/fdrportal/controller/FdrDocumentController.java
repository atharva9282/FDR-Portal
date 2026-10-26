package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.DocumentDownload;
import com.aicte.fdrportal.dto.FdrDocumentResponse;
import com.aicte.fdrportal.enums.DocumentType;
import com.aicte.fdrportal.service.FdrDocumentService;

import jakarta.validation.constraints.Positive;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/fdrs/{fdrId}/documents")
@Validated
public class FdrDocumentController {

    private final FdrDocumentService documentService;

    public FdrDocumentController(
            FdrDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FdrDocumentResponse> upload(
            @PathVariable @Positive Long fdrId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file) {

        FdrDocumentResponse response =
                documentService.upload(fdrId, documentType, file);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{documentId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<FdrDocumentResponse> findAll(
            @PathVariable @Positive Long fdrId) {

        return documentService.findByFdrId(fdrId);
    }

    @GetMapping("/{documentId}")
    public FdrDocumentResponse findById(
            @PathVariable @Positive Long fdrId,
            @PathVariable @Positive Long documentId) {

        return documentService.findById(fdrId, documentId);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable @Positive Long fdrId,
            @PathVariable @Positive Long documentId) {

        DocumentDownload document =
                documentService.download(fdrId, documentId);

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                document.originalFilename(),
                                StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        document.mediaType()))
                .contentLength(document.sizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(document.resource());
    }
}