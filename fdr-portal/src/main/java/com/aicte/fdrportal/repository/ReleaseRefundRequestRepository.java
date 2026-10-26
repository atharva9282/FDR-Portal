package com.aicte.fdrportal.repository;

import com.aicte.fdrportal.entity.ReleaseRefundRequest;
import com.aicte.fdrportal.enums.ReleaseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReleaseRefundRequestRepository
        extends JpaRepository<ReleaseRefundRequest, Long> {

    List<ReleaseRefundRequest> findByFdr_IdOrderByRequestedAtDesc(
            Long fdrId
    );

    boolean existsByFdr_IdAndStatusIn(
            Long fdrId,
            Collection<ReleaseRequestStatus> statuses
    );
}