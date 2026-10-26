package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.AuthResponse;
import com.aicte.fdrportal.dto.LoginRequest;
import com.aicte.fdrportal.dto.UserCreateRequest;
import com.aicte.fdrportal.dto.UserProfileResponse;
import com.aicte.fdrportal.service.AuthService;
import com.aicte.fdrportal.service.UserProvisioningService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserProvisioningService userProvisioningService;

    public AuthController(
            AuthService authService,
            UserProvisioningService userProvisioningService) {
        this.authService = authService;
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {

        return authService.me(authentication.getName());
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse createUser(
            @Valid @RequestBody UserCreateRequest request) {

        return userProvisioningService.create(request);
    }
}