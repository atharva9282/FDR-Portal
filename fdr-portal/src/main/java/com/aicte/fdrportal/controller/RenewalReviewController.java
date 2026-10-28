package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.RenewalResponse;
import com.aicte.fdrportal.dto.RenewalReviewRequest;
import com.aicte.fdrportal.service.RenewalService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/renewals")
@Validated
public class RenewalReviewController {

    private final RenewalService renewalService;

    public RenewalReviewController(RenewalService renewalService) {
        this.renewalService = renewalService;
    }

    @GetMapping
    public List<RenewalResponse> findAllForApprovalQueue() {
        return renewalService.findAllForApprovalQueue();
    }

    @PatchMapping("/{renewalId}/review")
    public RenewalResponse review(
            @PathVariable @Positive Long renewalId,
            @Valid @RequestBody RenewalReviewRequest request) {

        return renewalService.review(renewalId, request);
    }
}