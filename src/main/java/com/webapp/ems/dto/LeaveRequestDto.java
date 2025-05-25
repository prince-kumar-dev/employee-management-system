package com.webapp.ems.dto;

import com.webapp.ems.enums.LeaveStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LeaveRequestDto {
    private Long id;
    private Long employeeId;        // For request
    private String employeeName;    // For response
    private String employeeEmail;   // For response
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus status;
    private String adminRemarks;
    private String actionByAdminName; // For response
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}