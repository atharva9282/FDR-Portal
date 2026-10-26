package com.aicte.fdrportal.controller;

import com.aicte.fdrportal.dto.MaturityRecordResponse;
import com.aicte.fdrportal.dto.MonitoringDashboardResponse;
import com.aicte.fdrportal.dto.NotificationResponse;
import com.aicte.fdrportal.service.MonitoringService;
import com.aicte.fdrportal.service.NotificationService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@Validated
public class FdrMonitoringController {

    private final MonitoringService monitoringService;
    private final NotificationService notificationService;

    public FdrMonitoringController(
            MonitoringService monitoringService,
            NotificationService notificationService) {
        this.monitoringService = monitoringService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public MonitoringDashboardResponse dashboard(
            @RequestParam(defaultValue = "30")
            @Min(0) @Max(3650) int withinDays) {

        return monitoringService.getDashboard(withinDays);
    }

    @GetMapping("/maturities")
    public List<MaturityRecordResponse> maturities(
            @RequestParam(defaultValue = "30")
            @Min(0) @Max(3650) int withinDays) {

        return monitoringService.findMaturities(withinDays);
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications(
            @RequestParam(defaultValue = "false")
            boolean unreadOnly) {

        return notificationService.findAll(unreadOnly);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable @Min(1) Long notificationId) {

        return notificationService.markAsRead(notificationId);
    }
}