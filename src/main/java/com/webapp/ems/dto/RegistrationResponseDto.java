package com.webapp.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponseDto {
    private String message;
    private String email; // To help frontend identify user for OTP screen
    // You could add more fields if needed, like a temporary user ID if you don't want to expose email
}