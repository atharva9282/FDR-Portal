package com.aicte.fdrportal.repository;

import com.aicte.fdrportal.entity.VerificationReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationReviewRepository
        extends JpaRepository<VerificationReview, Long> {

    List<VerificationReview> findByFdr_IdOrderByReviewedAtDesc(
            Long fdrId
    );
}