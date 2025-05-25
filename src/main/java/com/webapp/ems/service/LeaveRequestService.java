package com.webapp.ems.service;

import com.webapp.ems.dto.LeaveRequestActionDto;
import com.webapp.ems.dto.LeaveRequestDto;
import com.webapp.ems.enums.LeaveStatus;
import com.webapp.ems.enums.Role;
import com.webapp.ems.model.LeaveRequest;
import com.webapp.ems.model.User;
import com.webapp.ems.repository.LeaveRequestRepository;
import com.webapp.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // --- Employee methods (applyForLeave, getLeaveRequestsByEmployeeId, cancelLeaveRequest) remain the same ---
    // ... (code for employee methods as before) ...
    @Transactional
    public LeaveRequestDto applyForLeave(LeaveRequestDto leaveRequestDto, Long employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

        if (leaveRequestDto.getStartDate() == null || leaveRequestDto.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required.");
        }
        if (leaveRequestDto.getStartDate().isAfter(leaveRequestDto.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        // Consider if start date can be today or must be in future
        if (leaveRequestDto.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setStartDate(leaveRequestDto.getStartDate());
        leaveRequest.setEndDate(leaveRequestDto.getEndDate());
        leaveRequest.setReason(leaveRequestDto.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        try {
            emailService.sendLeaveApplicationConfirmationToEmployee(savedRequest);
            List<User> adminsToNotify = userRepository.findAllByRole(Role.ADMIN); // Or get a specific list of HR/admins
            // For scoped admin, you might want to notify only the employee's direct managing admin
            if (employee.getManagedByAdmin() != null) {
                adminsToNotify = List.of(employee.getManagedByAdmin()); // Notify only the direct manager
            } else {
                // If employee has no direct manager, notify all admins (or a default HR group)
                adminsToNotify = userRepository.findAllByRole(Role.ADMIN);
            }

//            if (!adminsToNotify.isEmpty()) {
//                emailService.sendLeaveApplicationNotificationToAdmin(savedRequest, adminsToNotify);
//            }
        } catch (Exception e) {
            System.err.println("ERROR sending email notifications for new leave application (ID: " + savedRequest.getId() + "): " + e.getMessage());
        }
        return mapToDto(savedRequest);
    }

    public List<LeaveRequestDto> getLeaveRequestsByEmployeeId(Long employeeId) {
        if (!userRepository.existsById(employeeId)) {
            throw new RuntimeException("Employee not found with ID: " + employeeId);
        }
        return leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    @Transactional
    public LeaveRequestDto cancelLeaveRequest(Long leaveRequestId, Long employeeId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found."));

        if (!leaveRequest.getEmployee().getId().equals(employeeId)) {
            throw new SecurityException("You are not authorized to cancel this leave request.");
        }
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only PENDING leave requests can be cancelled. Current status: " + leaveRequest.getStatus());
        }
        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest cancelledRequest = leaveRequestRepository.save(leaveRequest);
        // Optional: Notify admin about cancellation
        return mapToDto(cancelledRequest);
    }


    // --- Admin Methods (Modified for Scoping) ---

    /**
     * Admin: Get all leave requests for employees managed by this admin, optionally filtered by status.
     */
    public List<LeaveRequestDto> getAllLeaveRequestsForAdmin(Long adminId, LeaveStatus status) {
        User admin = userRepository.findById(adminId)
                .filter(u -> u.getRole() == Role.ADMIN) // Ensure the user is an admin
                .orElseThrow(() -> new RuntimeException("Admin user not found or user is not an admin: " + adminId));

        List<LeaveRequest> requests;
        if (status != null) {
            requests = leaveRequestRepository.findAllByEmployee_ManagedByAdminAndStatus(admin, status);
        } else {
            requests = leaveRequestRepository.findAllByEmployee_ManagedByAdmin(admin);
        }
        return requests.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Admin: Get a specific leave request by its ID, ensuring it belongs to an employee they manage.
     */
    public LeaveRequestDto getLeaveRequestByIdForAdmin(Long leaveRequestId, Long adminId) {
        User admin = userRepository.findById(adminId)
                .filter(u -> u.getRole() == Role.ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin user not found or user is not an admin: " + adminId));

        LeaveRequest request = leaveRequestRepository.findByIdAndEmployee_ManagedByAdmin(leaveRequestId, admin)
                .orElseThrow(() -> new RuntimeException("Leave request not found or not managed by this admin. ID: " + leaveRequestId));
        return mapToDto(request);
    }

    /**
     * Admin: Update the status of a leave request for an employee they manage.
     */
    @Transactional
    public LeaveRequestDto updateLeaveStatusForAdmin(Long leaveRequestId, LeaveRequestActionDto actionDto, Long performingAdminId) {
        User admin = userRepository.findById(performingAdminId)
                .filter(u -> u.getRole() == Role.ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin user not found or user is not an admin: " + performingAdminId));

        // Fetch the leave request and ensure it belongs to an employee managed by this admin
        LeaveRequest leaveRequest = leaveRequestRepository.findByIdAndEmployee_ManagedByAdmin(leaveRequestId, admin)
                .orElseThrow(() -> new RuntimeException("Leave request not found or not managed by this admin. ID: " + leaveRequestId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Leave request can only be actioned if it is in PENDING status. Current status: " + leaveRequest.getStatus());
        }
        if (actionDto.getNewStatus() != LeaveStatus.APPROVED && actionDto.getNewStatus() != LeaveStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid action status. Must be APPROVED or REJECTED.");
        }

        leaveRequest.setStatus(actionDto.getNewStatus());
        leaveRequest.setAdminRemarks(actionDto.getAdminRemarks());
        leaveRequest.setActionByAdmin(admin); // The admin performing the action

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);

        try {
            emailService.sendLeaveStatusUpdateToEmployee(updatedRequest);
            System.out.println("Attempted to send leave status update email for request ID: " + updatedRequest.getId() + " to status " + updatedRequest.getStatus());
        } catch (Exception e) {
            System.err.println("ERROR sending email notification for leave status update (ID: " + updatedRequest.getId() + "): " + e.getMessage());
        }

        return mapToDto(updatedRequest);
    }

    // mapToDto remains the same
    private LeaveRequestDto mapToDto(LeaveRequest leaveRequest) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setId(leaveRequest.getId());
        if (leaveRequest.getEmployee() != null) {
            dto.setEmployeeId(leaveRequest.getEmployee().getId());
            dto.setEmployeeName(leaveRequest.getEmployee().getFirstName() + " " + leaveRequest.getEmployee().getLastName());
            dto.setEmployeeEmail(leaveRequest.getEmployee().getEmail());
        }
        dto.setStartDate(leaveRequest.getStartDate());
        dto.setEndDate(leaveRequest.getEndDate());
        dto.setReason(leaveRequest.getReason());
        dto.setStatus(leaveRequest.getStatus());
        dto.setAdminRemarks(leaveRequest.getAdminRemarks());
        if (leaveRequest.getActionByAdmin() != null) {
            dto.setActionByAdminName(leaveRequest.getActionByAdmin().getFirstName() + " " + leaveRequest.getActionByAdmin().getLastName());
        }
        dto.setCreatedAt(leaveRequest.getCreatedAt());
        dto.setUpdatedAt(leaveRequest.getUpdatedAt());
        return dto;
    }
}