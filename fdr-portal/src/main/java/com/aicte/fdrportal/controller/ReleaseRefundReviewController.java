package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.ReleaseRequestResponse;
import com.aicte.fdrportal.dto.ReleaseRequestReviewRequest;
import com.aicte.fdrportal.service.ReleaseRefundService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/release-requests")
@Validated
public class ReleaseRefundReviewController {

    private final ReleaseRefundService releaseRefundService;

    public ReleaseRefundReviewController(
            ReleaseRefundService releaseRefundService) {
        this.releaseRefundService = releaseRefundService;
    }

    @GetMapping
    public List<ReleaseRequestResponse> findAllForApprovalQueue() {
        return releaseRefundService.findAllForApprovalQueue();
    }

    @PatchMapping("/{requestId}/review")
    public ReleaseRequestResponse review(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody ReleaseRequestReviewRequest request) {

        return releaseRefundService.review(requestId, request);
    }
}