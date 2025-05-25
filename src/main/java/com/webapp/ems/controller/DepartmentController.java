package com.webapp.ems.controller;

import com.webapp.ems.dto.DepartmentDto;
import com.webapp.ems.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    // Helper to get adminId from header (same as in EmployeeController)
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

    @GetMapping("/my-managed")
    public ResponseEntity<?> getAllDepartmentsManagedByLoggedInAdmin(@RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr); // Your helper to parse header
            List<DepartmentDto> departments = departmentService.getAllDepartmentsByAdmin(adminId);
            return ResponseEntity.ok(departments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching departments for admin: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody DepartmentDto departmentDto,
                                              @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            DepartmentDto createdDepartment = departmentService.createDepartment(departmentDto, adminId);
            return new ResponseEntity<>(createdDepartment, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) { // For "already exists" type errors
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable Long id,
                                               @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            DepartmentDto departmentDto = departmentService.getDepartmentById(id, adminId);
            return ResponseEntity.ok(departmentDto);
        } catch (IllegalArgumentException e) { // For invalid adminId header
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
        catch (RuntimeException e) { // For "not found"
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping // Get all departments FOR A SPECIFIC ADMIN
    public ResponseEntity<?> getAllDepartmentsByAdmin(@RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            List<DepartmentDto> departments = departmentService.getAllDepartmentsByAdmin(adminId);
            return ResponseEntity.ok(departments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching departments.");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id,
                                              @RequestBody DepartmentDto departmentDto,
                                              @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            DepartmentDto updatedDepartment = departmentService.updateDepartment(id, departmentDto, adminId);
            return ResponseEntity.ok(updatedDepartment);
        } catch (IllegalArgumentException e) { // For invalid adminId header or DTO validation
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) { // For "not found" or "already exists"
            if (e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id,
                                              @RequestHeader("X-Admin-Id") String adminIdStr) {
        try {
            Long adminId = getPerformingAdminId(adminIdStr);
            departmentService.deleteDepartment(id, adminId);
            return ResponseEntity.ok("Department with ID " + id + " deleted successfully.");
        } catch (IllegalArgumentException e) { // For invalid adminId header
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) { // For "not found" or "cannot delete"
            if (e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage()); // e.g., if department has employees
        }
    }

    // --- PUBLIC ENDPOINT FOR REGISTRATION DROPDOWN ---
    @GetMapping("/all") // Maps to GET /api/departments
    public ResponseEntity<List<DepartmentDto>> getAllPublicDepartments() {
        List<DepartmentDto> departments = departmentService.getAllPublicDepartments();
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/api/selected-admin-departments") // Path is /api/departments. Frontend will add ?adminId=X or send X-Admin-Id header
    public ResponseEntity<?> getDepartmentsForSelectedAdmin(
            // Option A: Using a query parameter passed by frontend
            @RequestParam("adminId") Long adminId
            // Option B: Using the header (if you prefer)
            // @RequestHeader("X-Selected-Admin-Id") String selectedAdminIdStr
    ) {
        try {
            // Long adminIdToUse = (Option A) ? adminId : getPerformingAdminId(selectedAdminIdStr);
            List<DepartmentDto> departments = departmentService.getAllDepartmentsByAdmin(adminId); // Call the service method
            return ResponseEntity.ok(departments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching departments for selected admin.");
        }
    }
}