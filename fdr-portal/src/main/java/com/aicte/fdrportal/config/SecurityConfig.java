package com.aicte.fdrportal.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.List;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.security.jwt.secret}") String encodedSecret) {

        byte[] key = Base64.getDecoder().decode(encodedSecret);

        if (key.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must decode to at least 32 bytes");
        }

        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        JWKSource<SecurityContext> jwkSource =
                new ImmutableSecret<>(secretKey);

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken>
    jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, ? extends AbstractAuthenticationToken>
                    jwtAuthenticationConverter)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/actuator/health").permitAll()

                        .requestMatchers("/api/auth/login").permitAll()

                        .requestMatchers(
                                HttpMethod.POST, "/api/auth/users")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/auth/me")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST, "/api/institutions")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/institutions/**")
                        .hasAnyRole(
                                "ADMIN",
                                "VERIFICATION_OFFICIAL",
                                "APPROVING_AUTHORITY",
                                "REGULATION_OFFICIAL")

                        .requestMatchers(
                                "/api/fdrs/*/reviews",
                                "/api/fdrs/*/reviews/**")
                        .hasAnyRole(
                                "ADMIN",
                                "VERIFICATION_OFFICIAL",
                                "APPROVING_AUTHORITY",
                                "REGULATION_OFFICIAL")

                        .requestMatchers("/api/monitoring/**")
                        .hasAnyRole(
                                "ADMIN",
                                "INSTITUTION",
                                "VERIFICATION_OFFICIAL",
                                "APPROVING_AUTHORITY",
                                "REGULATION_OFFICIAL")

                        .requestMatchers(
                                HttpMethod.GET, "/api/fdrs/*/release-requests")
                        .hasAnyRole(
                                "ADMIN",
                                "INSTITUTION",
                                "VERIFICATION_OFFICIAL",
                                "APPROVING_AUTHORITY",
                                "REGULATION_OFFICIAL")

                        .requestMatchers("/api/release-requests/**")
                        .hasAnyRole(
                                "ADMIN",
                                "VERIFICATION_OFFICIAL",
                                "APPROVING_AUTHORITY",
                                "REGULATION_OFFICIAL")

                        .requestMatchers(
                                HttpMethod.GET, "/api/fdrs/*/renewals")
                        .hasAnyRole(
                                "ADMIN",
                                "INSTITUTION",
                                "VERIFICATION_OFFICIAL",
                                "APPROVING_AUTHORITY",
                                "REGULATION_OFFICIAL")

                        .requestMatchers(HttpMethod.POST, "/api/fdrs")
                        .hasAnyRole("ADMIN", "INSTITUTION")

                        .requestMatchers(HttpMethod.POST, "/api/fdrs/*/documents")
                        .hasAnyRole("ADMIN", "INSTITUTION")

                        .requestMatchers(HttpMethod.POST, "/api/fdrs/*/renewals")
                        .hasAnyRole("ADMIN", "INSTITUTION")

                        .requestMatchers(HttpMethod.POST, "/api/fdrs/*/release-requests")
                        .hasAnyRole("ADMIN", "INSTITUTION")

                        .requestMatchers("/api/fdrs/**")
                        .hasAnyRole(
                                "ADMIN",
                                "INSTITUTION",
                                "VERIFICATION_OFFICIAL",
                                "APPROVING_AUTHORITY",
                                "REGULATION_OFFICIAL")



                        .requestMatchers("/api/renewals/**")
                        .hasAnyRole(
                                "ADMIN",
                                "VERIFICATION_OFFICIAL",
                                "APPROVING_AUTHORITY",
                                "REGULATION_OFFICIAL")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                List.of("http://localhost:5173"));
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}