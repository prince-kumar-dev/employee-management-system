package com.webapp.ems.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final long OTP_VALID_DURATION_MINUTES = 10; // OTP valid for 10 minutes

    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10)); // Generates a digit between 0-9
        }
        return otp.toString();
    }

    public boolean isOtpExpired(LocalDateTime otpGeneratedTime) {
        if (otpGeneratedTime == null) {
            return true; // No generation time means it's invalid or expired
        }
        return otpGeneratedTime.plusMinutes(OTP_VALID_DURATION_MINUTES).isBefore(LocalDateTime.now());
    }
}