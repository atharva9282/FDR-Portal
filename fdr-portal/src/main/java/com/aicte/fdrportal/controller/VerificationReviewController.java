package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.VerificationReviewRequest;
import com.aicte.fdrportal.dto.VerificationReviewResponse;
import com.aicte.fdrportal.service.VerificationReviewService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fdrs/{fdrId}/reviews")
@Validated
public class VerificationReviewController {

    private final VerificationReviewService reviewService;

    public VerificationReviewController(
            VerificationReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<VerificationReviewResponse> create(
            @PathVariable @Positive Long fdrId,
            @Valid @RequestBody VerificationReviewRequest request) {

        VerificationReviewResponse response =
                reviewService.create(fdrId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{reviewId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<VerificationReviewResponse> findByFdrId(
            @PathVariable @Positive Long fdrId) {

        return reviewService.findByFdrId(fdrId);
    }
}