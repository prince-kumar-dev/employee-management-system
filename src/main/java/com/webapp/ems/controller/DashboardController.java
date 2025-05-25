package com.webapp.ems.controller;

import com.webapp.ems.dto.DashboardSummaryDto;
import com.webapp.ems.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    // Add helper getPerformingLoggedInAdminId if not already present in this controller
    private Long getPerformingLoggedInAdminId(String adminIdHeaderStr) {
        if (adminIdHeaderStr == null || adminIdHeaderStr.isEmpty()) {
            throw new IllegalArgumentException("Required header 'X-Admin-Id' is not present.");
        }
        try {
            return Long.parseLong(adminIdHeaderStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid X-Admin-Id header value.");
        }
    }

    @GetMapping("/summary") // Admin's dashboard summary
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(@RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingLoggedInAdminId(adminIdStr); // Use your helper
            DashboardSummaryDto summary = dashboardService.getDashboardSummary(adminId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Or an error DTO
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}