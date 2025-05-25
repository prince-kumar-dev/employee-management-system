package com.webapp.ems.dto;

import com.webapp.ems.enums.LeaveStatus;
import jakarta.validation.constraints.NotNull; // If you add validation
import lombok.Data;

@Data
public class LeaveRequestActionDto {
    @NotNull // Example validation
    private LeaveStatus newStatus; // Should be APPROVED or REJECTED
    private String adminRemarks;
}