package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.AuthResponse;
import com.aicte.fdrportal.dto.LoginRequest;
import com.aicte.fdrportal.dto.UserProfileResponse;
import com.aicte.fdrportal.entity.UserAccount;
import com.aicte.fdrportal.repository.UserAccountRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final UserAccountRepository userRepository;
    private final UserProvisioningService userProvisioningService;
    private final long tokenTtlMinutes;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            UserAccountRepository userRepository,
            UserProvisioningService userProvisioningService,
            @Value("${app.security.jwt.ttl-minutes:120}")
            long tokenTtlMinutes) {

        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.userProvisioningService = userProvisioningService;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        email, request.password());

        authenticationManager.authenticate(authentication);

        UserAccount user = userRepository
                .findWithInstitutionByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user was not found"));

        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenTtlMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("fdr-portal")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .claim("userId", user.getId())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        UserProfileResponse profile =
                userProvisioningService.toProfile(user);

        return new AuthResponse(
                token,
                "Bearer",
                expiresAt,
                profile
        );
    }

    public UserProfileResponse me(String email) {
        return userProvisioningService.findByEmail(email);
    }
}