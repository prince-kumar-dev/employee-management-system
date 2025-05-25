package com.webapp.ems.service;

import com.webapp.ems.enums.LeaveStatus;
import com.webapp.ems.model.LeaveRequest;
import com.webapp.ems.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}") // To use as the 'from' address
    private String fromEmailAddress;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy"); // e.g., October 26, 2023

    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(to);
            message.setSubject("Your EMS Account - Email Verification OTP");
            message.setText("Hi there,\n\nYour One-Time Password (OTP) for verifying your email address is: "
                    + otp
                    + "\n\nThis OTP is valid for 10 minutes."
                    + "\n\nIf you did not request this, please ignore this email."
                    + "\n\nRegards,\nTeam Connect");
            javaMailSender.send(message);
            System.out.println("OTP email sent successfully to " + to); // For server log
        } catch (Exception e) {
            System.err.println("Error sending OTP email to " + to + ": " + e.getMessage());
            // Depending on your error handling strategy, you might rethrow a custom exception
            // throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    public void sendWelcomeEmail(User employee) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(employee.getEmail());
            message.setSubject("Welcome to Our Employee Management System! Team Connect");

            StringBuilder sb = new StringBuilder();
            sb.append("Hi ").append(employee.getFirstName()).append(",\n\n");
            sb.append("An account has been created for you in our Employee Management System by an administrator.\n\n");
            sb.append("You can log in using your email and the password provided (or set by the admin).\n");
            sb.append("Your login email: ").append(employee.getEmail()).append("\n");
            sb.append("Your details on record:\n");
            sb.append("- Name: ").append(employee.getFirstName()).append(" ").append(employee.getLastName()).append("\n");
            if (employee.getDepartment() != null) {
                sb.append("- Department: ").append(employee.getDepartment().getName()).append("\n");
            }
            if (employee.getHireDate() != null) {
                sb.append("- Hire Date: ").append(employee.getHireDate().toString()).append("\n");
            }
            if (employee.getPassword() != null) {
                sb.append("- Password: ").append(employee.getPassword()).append("\n");
            }
            // Consider privacy before including salary in an email.
            // if (employee.getSalary() != null) {
            //     sb.append("- Salary: [Protected or Omitted for Email]\n");
            // }
            sb.append("\nRegards,\nAdmin Team");

            message.setText(sb.toString());
            javaMailSender.send(message);
            System.out.println("Welcome email sent successfully to " + employee.getEmail());
        } catch (Exception e) {
            System.err.println("Error sending welcome email to " + employee.getEmail() + ": " + e.getMessage());
        }
    }

    public void sendLeaveApplicationConfirmationToEmployee(LeaveRequest leaveRequest) {
        if (leaveRequest == null || leaveRequest.getEmployee() == null) {
            System.err.println("Cannot send leave application confirmation: leave request or employee is null.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(leaveRequest.getEmployee().getEmail());
            message.setSubject("Leave Request Submitted Successfully (ID: " + leaveRequest.getId() + ")");

            StringBuilder sb = new StringBuilder();
            sb.append("Hi ").append(leaveRequest.getEmployee().getFirstName()).append(",\n\n");
            sb.append("Your leave request has been successfully submitted and is now PENDING approval.\n\n");
            sb.append("Leave Details:\n");
            sb.append("- Request ID: ").append(leaveRequest.getId()).append("\n");
            sb.append("- Start Date: ").append(leaveRequest.getStartDate().format(dateFormatter)).append("\n");
            sb.append("- End Date: ").append(leaveRequest.getEndDate().format(dateFormatter)).append("\n");
            sb.append("- Reason: ").append(leaveRequest.getReason() != null ? leaveRequest.getReason() : "N/A").append("\n");
            sb.append("- Status: ").append(leaveRequest.getStatus()).append("\n\n");
            sb.append("You will be notified once your request has been reviewed by an administrator.\n");
            sb.append("\nRegards,\nEMS Team");

            message.setText(sb.toString());
            javaMailSender.send(message);
            System.out.println("Leave application confirmation email sent to " + leaveRequest.getEmployee().getEmail());
        } catch (Exception e) {
            System.err.println("Error sending leave application confirmation to " + leaveRequest.getEmployee().getEmail() + ": " + e.getMessage());
        }
    }

    /**
     * Sends an email to the employee when their leave request status is updated by an admin (Approved/Rejected).
     * @param leaveRequest The leave request with the updated status.
     */
    public void sendLeaveStatusUpdateToEmployee(LeaveRequest leaveRequest) {
        if (leaveRequest == null || leaveRequest.getEmployee() == null || leaveRequest.getStatus() == null) {
            System.err.println("Cannot send leave status update: critical leave request information is missing.");
            return;
        }
        // Only send for definitive statuses like APPROVED or REJECTED
        if (leaveRequest.getStatus() != LeaveStatus.APPROVED && leaveRequest.getStatus() != LeaveStatus.REJECTED) {
            System.out.println("Skipping leave status update email for status: " + leaveRequest.getStatus());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(leaveRequest.getEmployee().getEmail());
            message.setSubject("Update on Your Leave Request (ID: " + leaveRequest.getId() + ")");

            StringBuilder sb = new StringBuilder();
            sb.append("Hi ").append(leaveRequest.getEmployee().getFirstName()).append(",\n\n");
            sb.append("There has been an update on your leave request (ID: ").append(leaveRequest.getId()).append(").\n\n");
            sb.append("New Status: ").append(leaveRequest.getStatus()).append("\n\n");
            sb.append("Leave Details:\n");
            sb.append("- Start Date: ").append(leaveRequest.getStartDate().format(dateFormatter)).append("\n");
            sb.append("- End Date: ").append(leaveRequest.getEndDate().format(dateFormatter)).append("\n");
            sb.append("- Reason: ").append(leaveRequest.getReason() != null ? leaveRequest.getReason() : "N/A").append("\n");

            if (leaveRequest.getAdminRemarks() != null && !leaveRequest.getAdminRemarks().isEmpty()) {
                sb.append("- Admin Remarks: ").append(leaveRequest.getAdminRemarks()).append("\n");
            }
            if (leaveRequest.getActionByAdmin() != null) {
                sb.append("- Actioned By: Admin (").append(leaveRequest.getActionByAdmin().getFirstName()).append(")\n");
            }
            sb.append("\n\nIf you have any questions, please contact your administrator.\n");
            sb.append("\nRegards,\nEMS Team");

            message.setText(sb.toString());
            javaMailSender.send(message);
            System.out.println("Leave status update email sent to " + leaveRequest.getEmployee().getEmail() + " for status " + leaveRequest.getStatus());
        } catch (Exception e) {
            System.err.println("Error sending leave status update to " + leaveRequest.getEmployee().getEmail() + ": " + e.getMessage());
        }
    }
}