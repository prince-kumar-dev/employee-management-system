package com.webapp.ems.dto;

import lombok.Data;

@Data
public class OtpVerificationRequestDto {
    private String email;
    private String otp;
}