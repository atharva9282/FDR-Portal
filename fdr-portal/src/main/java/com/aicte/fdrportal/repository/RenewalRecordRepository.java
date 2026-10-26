package com.aicte.fdrportal.repository;

import com.aicte.fdrportal.entity.RenewalRecord;
import com.aicte.fdrportal.enums.RenewalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RenewalRecordRepository
        extends JpaRepository<RenewalRecord, Long> {

    List<RenewalRecord> findByOriginalFdr_IdOrderByCreatedAtDesc(
            Long originalFdrId
    );

    boolean existsByOriginalFdr_IdAndStatusIn(
            Long originalFdrId,
            Collection<RenewalRequestStatus> statuses
    );

    Optional<RenewalRecord> findByIdAndOriginalFdr_Id(
            Long id,
            Long originalFdrId
    );
}