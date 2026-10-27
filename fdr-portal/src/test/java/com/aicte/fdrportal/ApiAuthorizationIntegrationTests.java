package com.aicte.fdrportal;

import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.entity.UserAccount;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.repository.InstitutionRepository;
import com.aicte.fdrportal.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserAccountRepository userRepository;

    private String institutionEmail;

    @BeforeEach
    void createInstitutionAccount() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Institution institution = new Institution();
        institution.setInstitutionCode("IT-" + suffix);
        institution.setName("API Test Institution " + suffix);
        institution = institutionRepository.saveAndFlush(institution);

        institutionEmail = "institution-" + suffix + "@example.test";

        UserAccount account = new UserAccount();
        account.setEmail(institutionEmail);
        account.setPasswordHash("not-used-in-mock-jwt-test");
        account.setRole(UserRole.INSTITUTION);
        account.setInstitution(institution);
        account.setEnabled(true);
        userRepository.saveAndFlush(account);
    }

    @Test
    void unauthenticatedFdrRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/fdrs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void institutionCannotReadInstitutionDirectory() throws Exception {
        mockMvc.perform(get("/api/institutions")
                        .with(institutionJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void institutionCanReadScopedMonitoringDashboard() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard")
                        .param("withinDays", "30")
                        .with(institutionJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInstruments").value(0))
                .andExpect(jsonPath("$.unreadNotifications").value(0));
    }

    @Test
    void institutionCannotReadInternalVerificationHistory() throws Exception {
        mockMvc.perform(get("/api/fdrs/1/reviews")
                        .with(institutionJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidMonitoringWindowReturnsStructuredBadRequest() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard")
                        .param("withinDays", "-1")
                        .with(institutionJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private JwtRequestPostProcessor institutionJwt() {
        return jwt()
                .jwt(token -> token.subject(institutionEmail))
                .authorities(new SimpleGrantedAuthority("ROLE_INSTITUTION"));
    }
}
