package com.webapp.ems.service;

import com.webapp.ems.dto.*;
import com.webapp.ems.enums.Gender;
import com.webapp.ems.enums.Role;
import com.webapp.ems.model.Department;
import com.webapp.ems.model.User;
import com.webapp.ems.repository.DepartmentRepository; // If used during registration
import com.webapp.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository; // Keep if needed for self-reg of employee
    private final EmailService emailService;
    private final OtpService otpService;

    @Transactional
    public RegistrationResponseDto registerUser(EmployeeDto registrationRequest) {
        Optional<User> existingUserOpt = userRepository.findByEmail(registrationRequest.getEmail());
        if (existingUserOpt.isPresent()) {
            // ... (existing logic for handling existing users) ...
            User existingUser = existingUserOpt.get();
            if (existingUser.isVerified()) {
                throw new RuntimeException("Error: Email is already in use and verified.");
            }
        }

        User user = new User();
        user.setFirstName(registrationRequest.getFirstName());
        user.setLastName(registrationRequest.getLastName());
        user.setEmail(registrationRequest.getEmail());
        user.setPassword(registrationRequest.getPassword()); // NO HASHING

        if (registrationRequest.getGender() != null && !registrationRequest.getGender().toString().isEmpty()) {
            try {
                user.setGender(Gender.valueOf(registrationRequest.getGender().toString().toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid gender value during registration: " + registrationRequest.getGender());
                // Handle error or set to null/default
            }
        }


        Role role;
        try {
            role = Role.valueOf(registrationRequest.getRole().toUpperCase());
            user.setRole(role);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role specified: " + registrationRequest.getRole());
        }

        if (role == Role.EMPLOYEE) {
            user.setDateOfBirth(registrationRequest.getDateOfBirth());
            user.setHireDate(registrationRequest.getHireDate()); // Optional for self-reg, admin might fill later
            user.setSalary(registrationRequest.getSalary());     // Optional for self-reg

            if (registrationRequest.getDepartmentId() != null) {
                Department department = departmentRepository.findById(registrationRequest.getDepartmentId())
                        .orElseThrow(() -> new RuntimeException("Selected department not found."));
                user.setDepartment(department);
                // Optional: You might want to verify if this department belongs to the selected managing admin
            }

            // ***** NEW: Assign managedByAdmin for self-registering EMPLOYEE *****
            if (registrationRequest.getManagedByAdminId() != null) { // Frontend will send this
                User managingAdmin = userRepository.findById(registrationRequest.getManagedByAdminId())
                        .filter(adminUser -> adminUser.getRole() == Role.ADMIN) // Ensure selected user is an ADMIN
                        .orElseThrow(() -> new RuntimeException("Selected managing admin not found or is not an Admin."));
                user.setManagedByAdmin(managingAdmin);
            } else {
                // What to do if no admin is selected?
                // Option 1: Make it mandatory for employee self-registration.
                // Option 2: Assign to a default admin/unassigned pool (requires more logic).
                // Option 3: Leave it null, and an admin later assigns them.
                // For now, let's assume the frontend will ensure one is selected if required.
                // If it can be null, the DB column `managed_by_admin_id` must be nullable.
                // throw new RuntimeException("A managing admin must be selected for employee registration.");
                System.out.println("Warning: Employee self-registering without selecting a managing admin. managedByAdmin will be null.");
            }
        } else if (role == Role.ADMIN) {
            // Admins do not have a managedByAdmin (they are the manager or top-level)
            user.setManagedByAdmin(null);
        }

        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpGeneratedTime(LocalDateTime.now());
        user.setVerified(false);

        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), otp);

        return new RegistrationResponseDto("OTP sent to your email. Please verify to complete registration.", user.getEmail());
    }

    @Transactional
    public UserResponseDto verifyOtp(OtpVerificationRequestDto otpRequest) {
        User user = userRepository.findByEmail(otpRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + otpRequest.getEmail()));

        if (user.isVerified()) {
            throw new RuntimeException("Account is already verified.");
        }

        if (user.getOtp() == null || !user.getOtp().equals(otpRequest.getOtp())) {
            throw new RuntimeException("Invalid OTP.");
        }

        if (otpService.isOtpExpired(user.getOtpGeneratedTime())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        user.setVerified(true);
        user.setOtp(null); // Clear OTP after successful verification
        user.setOtpGeneratedTime(null);
        User savedUser = userRepository.save(user);

        return mapToUserResponseDto(savedUser); // Return full user DTO, ready for login state
    }

    @Transactional
    public String resendOtp(ResendOtpRequestDto resendOtpRequest) {
        User user = userRepository.findByEmail(resendOtpRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + resendOtpRequest.getEmail()));

        if (user.isVerified()) {
            throw new RuntimeException("Account is already verified.");
        }

        String newOtp = otpService.generateOtp();
        user.setOtp(newOtp);
        user.setOtpGeneratedTime(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), newOtp);
        return "A new OTP has been sent to your email.";
    }


    public UserResponseDto loginUser(AuthRequestDto loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + loginRequest.getEmail()));

        if (!user.getPassword().equals(loginRequest.getPassword())) { // NO HASHING
            throw new RuntimeException("Invalid email or password."); // Generic message for security
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Account not verified. Please check your email for OTP or resend OTP.");
        }

        return mapToUserResponseDto(user);
    }

    // This mapper is now more important as it's returned after successful OTP verification too
    private UserResponseDto mapToUserResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        if (user.getDepartment() != null) {
            dto.setDepartmentId(user.getDepartment().getId());
            dto.setDepartmentName(user.getDepartment().getName());
        }
        if(user.getGender() != null) {
            dto.setGender(user.getGender().name());
        }
        // Make sure these are in UserResponseDto and are populated
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setHireDate(user.getHireDate());
        dto.setSalary(user.getSalary());
        // Do NOT include OTP or isVerified in sensitive response DTOs usually,
        // but for `getLoggedInUser` in frontend, it might be useful to know verified status implicitly.
        // The token/session itself implies verification after login.
        return dto;
    }
}