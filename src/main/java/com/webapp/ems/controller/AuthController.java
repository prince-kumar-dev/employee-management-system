package com.webapp.ems.controller;

import com.webapp.ems.dto.*;
import com.webapp.ems.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody EmployeeDto registrationRequest) {
        try {
            RegistrationResponseDto response = authService.registerUser(registrationRequest);
            return ResponseEntity.status(HttpStatus.OK).body(response); // OK because next step is OTP
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Or Map.of("message", e.getMessage())
        }
    }

    // In AuthController.java
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationRequestDto otpRequest) {
        try {
            UserResponseDto userResponse = authService.verifyOtp(otpRequest);
            return ResponseEntity.ok(userResponse);
        } catch (RuntimeException e) {
            // Create a map or a dedicated ErrorResponseDto
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage()); // General message

            // You can add a specific error code based on the exception type or message content
            if (e.getMessage().toLowerCase().contains("invalid otp")) {
                errorResponse.put("errorCode", "INVALID_OTP");
            } else if (e.getMessage().toLowerCase().contains("otp has expired")) {
                errorResponse.put("errorCode", "OTP_EXPIRED");
            } else if (e.getMessage().toLowerCase().contains("user not found")) {
                errorResponse.put("errorCode", "USER_NOT_FOUND_FOR_OTP");
            }
            // ... add more specific error codes as needed

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequestDto resendOtpRequest) {
        try {
            String message = authService.resendOtp(resendOtpRequest);
            return ResponseEntity.ok(message); // Or Map.of("message", message)
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody AuthRequestDto loginRequest) {
        try {
            UserResponseDto userResponse = authService.loginUser(loginRequest);
            return ResponseEntity.ok(userResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}