package com.aicte.fdrportal.repository;

import com.aicte.fdrportal.entity.FdrDocument;
import com.aicte.fdrportal.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FdrDocumentRepository extends JpaRepository<FdrDocument, Long> {

    List<FdrDocument> findByFdr_IdOrderByUploadedAtDesc(Long fdrId);

    Optional<FdrDocument> findByIdAndFdr_Id(Long documentId, Long fdrId);

    boolean existsByFdr_IdAndDocumentType(
            Long fdrId,
            DocumentType documentType
    );
}
