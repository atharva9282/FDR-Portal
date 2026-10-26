package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.FdrCreateRequest;
import com.aicte.fdrportal.dto.FdrResponse;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.service.FdrInstrumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/fdrs")
public class FdrInstrumentController {

    private final FdrInstrumentService fdrService;

    public FdrInstrumentController(FdrInstrumentService fdrService) {
        this.fdrService = fdrService;
    }

    @PostMapping
    public ResponseEntity<FdrResponse> create( @Valid @RequestBody FdrCreateRequest request)
    {

        FdrResponse response = fdrService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public Page<FdrResponse> findAll(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) InstrumentStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "submittedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return fdrService.findAll(institutionId, status, pageable);
    }

    @GetMapping("/{id}")
    public FdrResponse findById(@PathVariable Long id) {
        return fdrService.findById(id);
    }
}
