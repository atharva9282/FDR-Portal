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
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private Long institutionId;

    @BeforeEach
    void createInstitutionAccount() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Institution institution = new Institution();
        institution.setInstitutionCode("IT-" + suffix);
        institution.setName("API Test Institution " + suffix);
        institution = institutionRepository.saveAndFlush(institution);
        institutionId = institution.getId();

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
    void approvingAuthorityCannotRegisterAnFdr() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/fdrs")
                        .with(jwt().jwt(token -> token.subject("approver@example.test"))
                                .authorities(new SimpleGrantedAuthority("ROLE_APPROVING_AUTHORITY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void institutionCanRegisterAnFdrAndReadItBack() throws Exception {
        String reference = uniqueReference();

        var result = mockMvc.perform(registerFdrRequest(reference))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instrumentReference").value(reference))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(location);
        String createdId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/fdrs/" + createdId)
                        .with(institutionJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrumentReference").value(reference));
    }

    @Test
    void duplicateBankAndReferenceReturnsConflict() throws Exception {
        String reference = uniqueReference();

        mockMvc.perform(registerFdrRequest(reference))
                .andExpect(status().isCreated());

        mockMvc.perform(registerFdrRequest(reference))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void missingFdrReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/fdrs/999999999")
                        .with(institutionJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void malformedPageParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/fdrs")
                        .param("page", "not-a-number")
                        .with(institutionJwt()))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    registerFdrRequest(String reference) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        String body = """
                {
                  "institutionId": %d,
                  "instrumentReference": "%s",
                  "bankName": "API Test Bank",
                  "branchName": "Main",
                  "depositAmount": 125000.00,
                  "issueDate": "%s",
                  "maturityDate": "%s"
                }
                """.formatted(institutionId, reference, today.minusDays(30), today.plusYears(1));

        return post("/api/fdrs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(institutionJwt());
    }

    private String uniqueReference() {
        return "API-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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
