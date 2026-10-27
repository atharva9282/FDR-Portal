package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.DocumentDownload;
import com.aicte.fdrportal.dto.FdrDocumentResponse;
import com.aicte.fdrportal.entity.FdrDocument;
import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.enums.DocumentType;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.exception.ResourceNotFoundException;
import com.aicte.fdrportal.repository.FdrDocumentRepository;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FdrDocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final FdrDocumentRepository documentRepository;
    private final FdrInstrumentRepository fdrRepository;
    private final InstitutionAccessService institutionAccessService;
    private final AuditEventService auditEventService;
    private final Path storageRoot;

    public FdrDocumentService(
            FdrDocumentRepository documentRepository,
            FdrInstrumentRepository fdrRepository,
            InstitutionAccessService institutionAccessService,
            AuditEventService auditEventService,
            @Value("${app.storage.upload-dir}") String uploadDir) {

        this.documentRepository = documentRepository;
        this.fdrRepository = fdrRepository;
        this.institutionAccessService = institutionAccessService;
        this.auditEventService = auditEventService;
        this.storageRoot = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    @Transactional
    public FdrDocumentResponse upload(Long fdrId, DocumentType documentType, MultipartFile file)
    {
        var actor = institutionAccessService.currentUser();
        if (actor.getRole() != UserRole.ADMIN
                && actor.getRole() != UserRole.INSTITUTION) {
            throw new AccessDeniedException(
                    "Only an institution user or administrator may upload documents");
        }

        if (documentType == null) {
            throw new IllegalArgumentException(
                    "Document type is required");
        }

        FdrInstrument fdr = fdrRepository.findById(fdrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FDR not found with ID: " + fdrId));

        institutionAccessService.assertCanAccessFdr(fdr);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a non-empty document");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Maximum document size is 10 MB");
        }

        String originalFilename =
                sanitizeFilename(file.getOriginalFilename());

        try {
            byte[] content = file.getBytes();

            DetectedFile detected = detectFileType(content);
            validateExtension(originalFilename, detected.extension());

            Files.createDirectories(storageRoot);

            // Generate a private server-side filename.
            String storageKey =
                    UUID.randomUUID() + detected.extension();

            Path destination = storageRoot.resolve(storageKey)
                    .normalize();

            if (!destination.startsWith(storageRoot)) {
                throw new IllegalArgumentException(
                        "Invalid storage path");
            }

            Files.write(
                    destination,
                    content,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            FdrDocument document = new FdrDocument();
            document.setFdr(fdr);
            document.setDocumentType(documentType);
            document.setOriginalFilename(originalFilename);
            document.setStorageKey(storageKey);
            document.setMediaType(detected.mediaType());
            document.setSizeBytes(content.length);
            document.setSha256(calculateSha256(content));

            try {
                FdrDocument saved = documentRepository.save(document);
                auditEventService.recordSuccess(
                        "FDR_DOCUMENT_UPLOADED", "FDR_DOCUMENT", saved.getId(),
                        "fdrId=" + fdr.getId()
                                + "; documentType=" + saved.getDocumentType()
                                + "; sizeBytes=" + saved.getSizeBytes());
                return toResponse(saved);
            } catch (RuntimeException ex) {
                // Avoid leaving a file behind if its DB record
                // cannot be saved.
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException cleanupException) {
                    ex.addSuppressed(cleanupException);
                }
                throw ex;
            }

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to store the uploaded document", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<FdrDocumentResponse> findByFdrId(Long fdrId) {

        FdrInstrument fdr = fdrRepository.findById(fdrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FDR not found with ID: " + fdrId));

        institutionAccessService.assertCanAccessFdr(fdr);

        return documentRepository
                .findByFdr_IdOrderByUploadedAtDesc(fdrId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FdrDocumentResponse findById(
            Long fdrId,
            Long documentId) {

        return toResponse(getDocument(fdrId, documentId));
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(
            Long fdrId,
            Long documentId) {

        FdrDocument document = getDocument(fdrId, documentId);

        Path storedFile = storageRoot.resolve(document.getStorageKey())
                .normalize();

        if (!storedFile.startsWith(storageRoot)
                || !Files.isRegularFile(storedFile)) {
            throw new ResourceNotFoundException(
                    "Stored document file was not found");
        }

        return new DocumentDownload(
                new FileSystemResource(storedFile),
                document.getOriginalFilename(),
                document.getMediaType(),
                document.getSizeBytes()
        );
    }

    private FdrDocument getDocument(
            Long fdrId,
            Long documentId) {

        FdrDocument document = documentRepository
                .findByIdAndFdr_Id(documentId, fdrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found for the specified FDR"));

        institutionAccessService.assertCanAccessFdr(
                document.getFdr());

        return document;
    }

    private FdrDocumentResponse toResponse(FdrDocument document) {

        return new FdrDocumentResponse(
                document.getId(),
                document.getFdr().getId(),
                document.getDocumentType(),
                document.getOriginalFilename(),
                document.getMediaType(),
                document.getSizeBytes(),
                document.getSha256(),
                document.getUploadedAt()
        );
    }

    private String sanitizeFilename(String filename) {

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Original filename is required");
        }

        // Strip both Unix and Windows directory components.
        String normalized = filename.replace('\\', '/');
        String leaf = normalized.substring(
                normalized.lastIndexOf('/') + 1);

        // Remove control characters to prevent unsafe header values.
        leaf = leaf.replaceAll("[\\p{Cntrl}]", "_").trim();

        if (leaf.isBlank() || leaf.length() > 255
                || leaf.equals(".") || leaf.equals("..")) {
            throw new IllegalArgumentException(
                    "Invalid filename");
        }

        return leaf;
    }

    private DetectedFile detectFileType(byte[] bytes) {

        // PDF: %PDF-
        if (bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-') {
            return new DetectedFile(".pdf", "application/pdf");
        }

        // PNG signature
        byte[] pngSignature = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        if (bytes.length >= pngSignature.length) {
            boolean matches = true;

            for (int i = 0; i < pngSignature.length; i++) {
                if (bytes[i] != pngSignature[i]) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return new DetectedFile(".png", "image/png");
            }
        }

        // JPEG signature
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return new DetectedFile(".jpg", "image/jpeg");
        }

        throw new IllegalArgumentException(
                "Unsupported file content. Upload a PDF, PNG, or JPEG document.");
    }

    private void validateExtension(
            String filename,
            String detectedExtension) {

        int dot = filename.lastIndexOf('.');

        if (dot < 0) {
            throw new IllegalArgumentException(
                    "The document must have a file extension");
        }

        String extension = filename.substring(dot)
                .toLowerCase(Locale.ROOT);

        boolean matches = extension.equals(detectedExtension)
                || (detectedExtension.equals(".jpg")
                && extension.equals(".jpeg"));

        if (!matches) {
            throw new IllegalArgumentException(
                    "File extension does not match its detected content");
        }
    }

    private String calculateSha256(byte[] content) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", ex);
        }
    }

    private record DetectedFile(
            String extension,
            String mediaType) {
    }
}