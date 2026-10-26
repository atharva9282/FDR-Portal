package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.RenewalCreateRequest;
import com.aicte.fdrportal.dto.RenewalResponse;
import com.aicte.fdrportal.service.RenewalService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fdrs/{fdrId}/renewals")
@Validated
public class RenewalController {

    private final RenewalService renewalService;

    public RenewalController(RenewalService renewalService) {
        this.renewalService = renewalService;
    }

    @PostMapping
    public ResponseEntity<RenewalResponse> create(
            @PathVariable @Positive Long fdrId,
            @Valid @RequestBody RenewalCreateRequest request) {

        RenewalResponse response =
                renewalService.create(fdrId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{renewalId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<RenewalResponse> findByOriginalFdrId(
            @PathVariable @Positive Long fdrId) {

        return renewalService.findByOriginalFdrId(fdrId);
    }
}