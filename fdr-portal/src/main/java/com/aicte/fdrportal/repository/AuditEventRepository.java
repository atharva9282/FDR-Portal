package com.aicte.fdrportal.repository;

import com.aicte.fdrportal.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            select event
            from AuditEvent event
            where (:fromTime is null or event.occurredAt >= :fromTime)
              and (:toTime is null or event.occurredAt <= :toTime)
              and (:actorEmail is null or lower(event.actorEmail) like lower(concat('%', :actorEmail, '%')))
              and (:action is null or lower(event.action) like lower(concat('%', :action, '%')))
              and (:targetType is null or lower(event.targetType) like lower(concat('%', :targetType, '%')))
            """)
    Page<AuditEvent> search(
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            @Param("actorEmail") String actorEmail,
            @Param("action") String action,
            @Param("targetType") String targetType,
            Pageable pageable
    );
}
