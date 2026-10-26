package com.aicte.fdrportal.repository;

import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FdrInstrumentRepository extends JpaRepository<FdrInstrument, Long> {

    Page<FdrInstrument> findByInstitution_Id(Long institutionId, Pageable pageable);

    List<FdrInstrument> findAllByInstitution_Id(Long institutionId);

    Page<FdrInstrument> findByStatus(InstrumentStatus status, Pageable pageable);

    Page<FdrInstrument> findByInstitution_IdAndStatus(
            Long institutionId,
            InstrumentStatus status,
            Pageable pageable
    );

    List<FdrInstrument> findByMaturityDateBetween(LocalDate startDate, LocalDate endDate);

    List<FdrInstrument> findByVerificationStatus(VerificationStatus verificationStatus);

    List<FdrInstrument> findByBankNameIgnoreCaseAndInstrumentReferenceIgnoreCase(
            String bankName,
            String instrumentReference
    );

    boolean existsByBankNameIgnoreCaseAndInstrumentReferenceIgnoreCase(
            String bankName,
            String instrumentReference
    );
}
