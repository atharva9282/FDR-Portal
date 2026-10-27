package com.aicte.fdrportal;

import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.entity.UserAccount;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.repository.InstitutionRepository;
import com.aicte.fdrportal.repository.UserAccountRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    private static final Path TEST_STORAGE_DIRECTORY = Path.of(
            System.getProperty("java.io.tmpdir"),
            "fdr-portal-test-" + UUID.randomUUID());

    private String institutionEmail;
    private String verificationEmail;
    private String approvingEmail;
    private Long institutionId;

    @DynamicPropertySource
    static void configureTestStorage(DynamicPropertyRegistry registry) {
        registry.add("app.storage.upload-dir", TEST_STORAGE_DIRECTORY::toString);
    }

    @AfterAll
    static void deleteTestStorage() throws IOException {
        if (Files.exists(TEST_STORAGE_DIRECTORY)) {
            try (var paths = Files.walk(TEST_STORAGE_DIRECTORY)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

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

        verificationEmail = "verifier-" + suffix + "@example.test";
        UserAccount verificationAccount = new UserAccount();
        verificationAccount.setEmail(verificationEmail);
        verificationAccount.setPasswordHash("not-used-in-mock-jwt-test");
        verificationAccount.setRole(UserRole.VERIFICATION_OFFICIAL);
        verificationAccount.setEnabled(true);
        userRepository.saveAndFlush(verificationAccount);

        approvingEmail = "approver-" + suffix + "@example.test";
        UserAccount approvingAccount = new UserAccount();
        approvingAccount.setEmail(approvingEmail);
        approvingAccount.setPasswordHash("not-used-in-mock-jwt-test");
        approvingAccount.setRole(UserRole.APPROVING_AUTHORITY);
        approvingAccount.setEnabled(true);
        userRepository.saveAndFlush(approvingAccount);
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
    void invalidStatusFilterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/fdrs")
                        .param("status", "not-a-status")
                        .with(institutionJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
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
    void institutionCanUploadAndDownloadPdfDocument() throws Exception {
        String fdrId = createFdr(uniqueReference());
        byte[] pdfBytes = "%PDF-1.4\nFDR Portal integration test\n%%EOF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "fdr-copy.pdf", "application/pdf", pdfBytes);

        var result = mockMvc.perform(multipart("/api/fdrs/{fdrId}/documents", fdrId)
                        .file(file)
                        .param("documentType", "FDR_COPY")
                        .with(institutionJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentType").value("FDR_COPY"))
                .andExpect(jsonPath("$.sha256").isNotEmpty())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(location);
        String documentId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/fdrs/{fdrId}/documents/{documentId}/download",
                        fdrId, documentId)
                        .with(institutionJwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void documentWithUnsupportedContentReturnsBadRequest() throws Exception {
        String fdrId = createFdr(uniqueReference());
        MockMultipartFile file = new MockMultipartFile(
                "file", "spoofed.pdf", "application/pdf",
                "this is not a PDF".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/fdrs/{fdrId}/documents", fdrId)
                        .file(file)
                        .param("documentType", "FDR_COPY")
                        .with(institutionJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void officialCanVerifyAnFdrAndActivateIt() throws Exception {
        String fdrId = createFdr(uniqueReference());
        String body = """
                {
                  "outcome": "VERIFIED_BY_OFFICIAL",
                  "verificationMethod": "OFFICIAL_DOCUMENT_REVIEW",
                  "sourceReference": "DOC-REVIEW-TEST",
                  "remarks": "Supporting document reviewed for this integration test"
                }
                """;

        mockMvc.perform(post("/api/fdrs/{fdrId}/reviews", fdrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(verificationJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("VERIFIED_BY_OFFICIAL"));

        mockMvc.perform(get("/api/fdrs/{fdrId}", fdrId)
                        .with(institutionJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED_BY_OFFICIAL"));
    }

    @Test
    void externalVerificationRequiresAnEvidenceReference() throws Exception {
        String fdrId = createFdr(uniqueReference());
        String body = """
                {
                  "outcome": "EXTERNALLY_CONFIRMED",
                  "verificationMethod": "BANK_ISSUED_CONFIRMATION",
                  "remarks": "Reference intentionally omitted for validation test"
                }
                """;

        mockMvc.perform(post("/api/fdrs/{fdrId}/reviews", fdrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(verificationJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String createFdr(String reference) throws Exception {
        var result = mockMvc.perform(registerFdrRequest(reference))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(location);
        return location.substring(location.lastIndexOf('/') + 1);
    }

    @Test
    void releaseWorkflowRequiresReviewApprovalAndCompletionReference() throws Exception {
        String fdrId = createVerifiedActiveFdr();
        String submitBody = """
                {"reason": "Release integration test"}
                """;

        var submitted = mockMvc.perform(post("/api/fdrs/{fdrId}/release-requests", fdrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody)
                        .with(institutionJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();

        String location = submitted.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(location);
        String requestId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(patch("/api/release-requests/{requestId}/review", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "UNDER_REVIEW", "remarks": "Initial review"}
                                """)
                        .with(verificationJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(patch("/api/release-requests/{requestId}/review", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "APPROVED", "remarks": "Approval recorded"}
                                """)
                        .with(approvingJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(patch("/api/release-requests/{requestId}/review", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "COMPLETED", "remarks": "Release recorded"}
                                """)
                        .with(approvingJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(patch("/api/release-requests/{requestId}/review", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus": "COMPLETED",
                                  "remarks": "Release recorded",
                                  "completionReference": "BANK-RELEASE-REF-TEST"
                                }
                                """)
                        .with(approvingJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/fdrs/{fdrId}", fdrId).with(institutionJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    void renewalApprovalRequiresReplacementDocument() throws Exception {
        RenewalScenario scenario = createRenewalUnderReview();

        mockMvc.perform(patch("/api/renewals/{renewalId}/review", scenario.renewalId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "APPROVED", "remarks": "Approval without replacement document"}
                                """)
                        .with(approvingJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void renewalCanBeApprovedAfterReplacementDocumentUploaded() throws Exception {
        RenewalScenario scenario = createRenewalUnderReview();

        uploadPdf(scenario.replacementFdrId(), "RENEWAL_REPLACEMENT");

        mockMvc.perform(patch("/api/renewals/{renewalId}/review", scenario.renewalId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "APPROVED", "remarks": "Replacement document checked"}
                                """)
                        .with(approvingJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/fdrs/{fdrId}", scenario.originalFdrId())
                        .with(institutionJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RENEWED"));

        mockMvc.perform(get("/api/fdrs/{fdrId}", scenario.replacementFdrId())
                        .with(institutionJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    private RenewalScenario createRenewalUnderReview() throws Exception {
        String fdrId = createVerifiedActiveFdr();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        String replacementReference = uniqueReference();
        String body = """
                {
                  "instrumentReference": "%s",
                  "bankName": "API Test Bank",
                  "branchName": "Main",
                  "depositAmount": 135000.00,
                  "issueDate": "%s",
                  "maturityDate": "%s",
                  "remarks": "Renewal integration test"
                }
                """.formatted(replacementReference, today.minusDays(1), today.plusYears(2));

        var submitted = mockMvc.perform(post("/api/fdrs/{fdrId}/renewals", fdrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(institutionJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();

        String renewalLocation = submitted.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(renewalLocation);
        String renewalId = renewalLocation.substring(renewalLocation.lastIndexOf('/') + 1);
        Object replacementFdrIdValue = JsonPath.read(
                submitted.getResponse().getContentAsString(), "$.replacementFdrId");
        String replacementFdrId = String.valueOf(replacementFdrIdValue);

        mockMvc.perform(patch("/api/renewals/{renewalId}/review", renewalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "UNDER_REVIEW", "remarks": "Initial review"}
                                """)
                        .with(verificationJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        return new RenewalScenario(fdrId, renewalId, replacementFdrId);
    }

    private String createVerifiedActiveFdr() throws Exception {
        String fdrId = createFdr(uniqueReference());
        String body = """
                {
                  "outcome": "VERIFIED_BY_OFFICIAL",
                  "verificationMethod": "OFFICIAL_DOCUMENT_REVIEW",
                  "sourceReference": "DOC-REVIEW-TEST",
                  "remarks": "Supporting document reviewed for workflow test"
                }
                """;

        mockMvc.perform(post("/api/fdrs/{fdrId}/reviews", fdrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(verificationJwt()))
                .andExpect(status().isCreated());

        return fdrId;
    }

    private void uploadPdf(String fdrId, String documentType) throws Exception {
        byte[] pdfBytes = "%PDF-1.4\nFDR Portal workflow test\n%%EOF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "replacement-fdr.pdf", "application/pdf", pdfBytes);

        mockMvc.perform(multipart("/api/fdrs/{fdrId}/documents", fdrId)
                        .file(file)
                        .param("documentType", documentType)
                        .with(institutionJwt()))
                .andExpect(status().isCreated());
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

    private JwtRequestPostProcessor approvingJwt() {
        return jwt()
                .jwt(token -> token.subject(approvingEmail))
                .authorities(new SimpleGrantedAuthority("ROLE_APPROVING_AUTHORITY"));
    }

    private JwtRequestPostProcessor verificationJwt() {
        return jwt()
                .jwt(token -> token.subject(verificationEmail))
                .authorities(new SimpleGrantedAuthority("ROLE_VERIFICATION_OFFICIAL"));
    }

    private JwtRequestPostProcessor institutionJwt() {
        return jwt()
                .jwt(token -> token.subject(institutionEmail))
                .authorities(new SimpleGrantedAuthority("ROLE_INSTITUTION"));
    }

    private record RenewalScenario(
            String originalFdrId,
            String renewalId,
            String replacementFdrId) {
    }
}
