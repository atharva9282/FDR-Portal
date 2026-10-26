package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.InstitutionCreateRequest;
import com.aicte.fdrportal.dto.InstitutionResponse;
import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.service.InstitutionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @PostMapping
    public ResponseEntity<InstitutionResponse> create(
            @Valid @RequestBody InstitutionCreateRequest request
            ){

        InstitutionResponse response = institutionService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public Page<InstitutionResponse> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {

        return institutionService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public InstitutionResponse findById(@PathVariable Long id) {
        return institutionService.findById(id);
    }

}
