package com.webapp.ems.controller;

import com.webapp.ems.dto.LeaveRequestActionDto;
import com.webapp.ems.dto.LeaveRequestDto;
import com.webapp.ems.enums.LeaveStatus;
import com.webapp.ems.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    // Helper to get admin ID from header (INSECURE - for non-Spring Security setup)
    private Long getPerformingAdminIdFromHeader(String adminIdHeaderStr) {
        if (adminIdHeaderStr == null || adminIdHeaderStr.isEmpty()) {
            throw new IllegalArgumentException("X-Admin-Id header is missing for admin operation.");
        }
        try {
            return Long.parseLong(adminIdHeaderStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid X-Admin-Id header value.");
        }
    }

    // --- Employee Endpoints (remain the same) ---
    // ... (POST /apply, GET /my-requests/{employeeId}, PUT /my-requests/{leaveRequestId}/cancel/{employeeId}) ...
    @PostMapping("/apply")
    public ResponseEntity<?> applyForLeave(@RequestBody LeaveRequestDto leaveRequestDto) {
        if (leaveRequestDto.getEmployeeId() == null) {
            return ResponseEntity.badRequest().body("Employee ID is required to apply for leave.");
        }
        try {
            LeaveRequestDto createdLeaveRequest = leaveRequestService.applyForLeave(leaveRequestDto, leaveRequestDto.getEmployeeId());
            return new ResponseEntity<>(createdLeaveRequest, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/my-requests/{employeeId}")
    public ResponseEntity<?> getMyLeaveRequests(@PathVariable Long employeeId) {
        try {
            List<LeaveRequestDto> requests = leaveRequestService.getLeaveRequestsByEmployeeId(employeeId);
            return ResponseEntity.ok(requests);
        } catch (RuntimeException e) {
            // Catch specific "not found" for employee if needed, or general error
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/my-requests/{leaveRequestId}/cancel/{employeeId}")
    public ResponseEntity<?> cancelMyLeaveRequest(@PathVariable Long leaveRequestId, @PathVariable Long employeeId) {
        try {
            LeaveRequestDto cancelledRequest = leaveRequestService.cancelLeaveRequest(leaveRequestId, employeeId);
            return ResponseEntity.ok(cancelledRequest);
        } catch (SecurityException e) { // For unauthorized access
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) { // For "not found" or "not pending"
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // --- Admin Endpoints (Modified for Scoping) ---
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllLeaveRequestsForAdmin(@RequestHeader("X-Admin-Id") String adminIdStr,
                                                         @RequestParam(required = false) LeaveStatus status) {
        try {
            Long adminId = getPerformingAdminIdFromHeader(adminIdStr);
            List<LeaveRequestDto> requests = leaveRequestService.getAllLeaveRequestsForAdmin(adminId, status);
            return ResponseEntity.ok(requests);
        } catch (IllegalArgumentException e) { // Catches issues from getPerformingAdminIdFromHeader
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) { // Catches "Admin not found" from service
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/admin/{leaveRequestId}")
    public ResponseEntity<?> getLeaveRequestByIdForAdmin(@PathVariable Long leaveRequestId,
                                                         @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminIdFromHeader(adminIdStr);
            LeaveRequestDto request = leaveRequestService.getLeaveRequestByIdForAdmin(leaveRequestId, adminId);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) { // Catches "not found or not managed" or "admin not found"
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/admin/{leaveRequestId}/action")
    public ResponseEntity<?> actionLeaveRequest(@PathVariable Long leaveRequestId,
                                                @RequestBody LeaveRequestActionDto actionDto,
                                                @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long performingAdminId = getPerformingAdminIdFromHeader(adminIdStr);
            LeaveRequestDto updatedLeaveRequest = leaveRequestService.updateLeaveStatusForAdmin(leaveRequestId, actionDto, performingAdminId);
            return ResponseEntity.ok(updatedLeaveRequest);
        } catch (IllegalArgumentException e) { // Catches invalid X-Admin-Id or invalid action status
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) { // Catches "not found or not managed", "already actioned", "admin not found"
            // For "already actioned" or state conflicts, 409 Conflict is appropriate
            // For "not found" or "not managed", 404 Not Found might be better if you distinguish
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}