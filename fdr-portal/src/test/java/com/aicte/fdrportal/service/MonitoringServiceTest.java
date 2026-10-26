package com.aicte.fdrportal.service;

import com.aicte.fdrportal.dto.MonitoringDashboardResponse;
import com.aicte.fdrportal.entity.FdrInstrument;
import com.aicte.fdrportal.entity.Institution;
import com.aicte.fdrportal.enums.InstrumentStatus;
import com.aicte.fdrportal.enums.VerificationStatus;
import com.aicte.fdrportal.repository.FdrInstrumentRepository;
import com.aicte.fdrportal.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringServiceTest {

    @Test
    void institutionDashboardUsesOnlyItsOwnInstrumentsAndNotifications() {
        FdrInstrumentRepository fdrRepository = mock(FdrInstrumentRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        InstitutionAccessService accessService = mock(InstitutionAccessService.class);

        Long institutionId = 73L;
        when(accessService.institutionScopeOrNull()).thenReturn(institutionId);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        Institution institution = new Institution();
        institution.setInstitutionCode("TEST-73");
        institution.setName("Test Institution");

        FdrInstrument fdr = new FdrInstrument();
        fdr.setInstitution(institution);
        fdr.setInstrumentReference("FDR-TEST-1");
        fdr.setBankName("Test Bank");
        fdr.setDepositAmount(new BigDecimal("1000.00"));
        fdr.setIssueDate(today.minusDays(365));
        fdr.setMaturityDate(today.plusDays(10));
        fdr.setStatus(InstrumentStatus.ACTIVE);
        fdr.setVerificationStatus(VerificationStatus.PENDING);

        when(fdrRepository.findAllByInstitution_Id(institutionId))
                .thenReturn(List.of(fdr));
        when(notificationRepository.countByFdr_Institution_IdAndReadFalse(institutionId))
                .thenReturn(2L);

        MonitoringService service = new MonitoringService(
                fdrRepository,
                notificationRepository,
                accessService,
                30
        );

        MonitoringDashboardResponse result = service.getDashboard(30);

        assertEquals(1L, result.totalInstruments());
        assertEquals(1L, result.activeInstruments());
        assertEquals(1L, result.expiringWithinWindow());
        assertEquals(1L, result.pendingVerifications());
        assertEquals(2L, result.unreadNotifications());

        verify(fdrRepository).findAllByInstitution_Id(institutionId);
        verify(fdrRepository, never()).findAll();
        verify(notificationRepository).countByFdr_Institution_IdAndReadFalse(institutionId);
        verify(notificationRepository, never()).countByReadFalse();
    }
}
