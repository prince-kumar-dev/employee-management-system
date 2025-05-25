package com.webapp.ems.controller;

import com.webapp.ems.dto.EmployeeDto;
import com.webapp.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees") // This path is now effectively "/api/admins/{adminId}/employees" conceptually
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // Admin ID passed via custom header (NOT SECURE FOR PRODUCTION)
    private Long getPerformingAdminId(String adminIdHeaderStr) {
        if (adminIdHeaderStr == null || adminIdHeaderStr.isEmpty()) {
            throw new IllegalArgumentException("X-Admin-Id header is missing.");
        }
        try {
            return Long.parseLong(adminIdHeaderStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid X-Admin-Id header value.");
        }
    }

    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeDto employeeDto,
                                            @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            EmployeeDto createdEmployee = employeeService.createEmployee(employeeDto, adminId);
            return new ResponseEntity<>(createdEmployee, HttpStatus.CREATED);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) { // Catch other runtime exceptions like "email in use"
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<?> getEmployeeById(@PathVariable Long employeeId,
                                             @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            EmployeeDto employeeDto = employeeService.getEmployeeByIdForAdmin(employeeId, adminId);
            return ResponseEntity.ok(employeeDto);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) { // For "not found"
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping // Get all employees FOR A SPECIFIC ADMIN
    public ResponseEntity<?> getAllEmployeesForAdmin(@RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            List<EmployeeDto> employees = employeeService.getAllEmployeesForAdmin(adminId);
            return ResponseEntity.ok(employees);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching employees.");
        }
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long employeeId,
                                            @RequestBody EmployeeDto employeeDto,
                                            @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            EmployeeDto updatedEmployee = employeeService.updateEmployee(employeeId, employeeDto, adminId);
            return ResponseEntity.ok(updatedEmployee);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // e.g. email exists
        }
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long employeeId,
                                            @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            employeeService.deleteEmployee(employeeId, adminId);
            return ResponseEntity.ok("Employee with ID " + employeeId + " deleted successfully.");
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}