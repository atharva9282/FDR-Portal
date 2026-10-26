package com.aicte.fdrportal.repository;

import com.aicte.fdrportal.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    boolean existsByAlertKey(String alertKey);

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByReadFalseOrderByCreatedAtDesc();

    List<Notification> findAllByFdr_Institution_IdOrderByCreatedAtDesc(
            Long institutionId
    );

    List<Notification> findByFdr_Institution_IdAndReadFalseOrderByCreatedAtDesc(
            Long institutionId
    );

    long countByReadFalse();

    long countByFdr_Institution_IdAndReadFalse(Long institutionId);
}