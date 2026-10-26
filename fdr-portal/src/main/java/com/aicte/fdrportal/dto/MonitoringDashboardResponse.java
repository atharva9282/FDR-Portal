package com.aicte.fdrportal.dto;

public record MonitoringDashboardResponse(
        int windowDays,
        long totalInstruments,
        long activeInstruments,
        long expiringWithinWindow,
        long maturedInstruments,
        long renewedInstruments,
        long releasedInstruments,
        long pendingVerifications,
        long discrepancyCases,
        long unreadNotifications
) {
}