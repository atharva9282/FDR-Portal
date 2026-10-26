package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.ReleaseRequestCreateRequest;
import com.aicte.fdrportal.dto.ReleaseRequestResponse;
import com.aicte.fdrportal.service.ReleaseRefundService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fdrs/{fdrId}/release-requests")
@Validated
public class ReleaseRefundController {

    private final ReleaseRefundService releaseRefundService;

    public ReleaseRefundController(
            ReleaseRefundService releaseRefundService) {
        this.releaseRefundService = releaseRefundService;
    }

    @PostMapping
    public ResponseEntity<ReleaseRequestResponse> submit(
            @PathVariable @Positive Long fdrId,
            @Valid @RequestBody ReleaseRequestCreateRequest request) {

        ReleaseRequestResponse response =
                releaseRefundService.submit(fdrId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{requestId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<ReleaseRequestResponse> findByFdrId(
            @PathVariable @Positive Long fdrId) {

        return releaseRefundService.findByFdrId(fdrId);
    }
}