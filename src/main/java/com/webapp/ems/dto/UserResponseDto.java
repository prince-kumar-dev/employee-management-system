package com.webapp.ems.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String gender;
    private Long departmentId;
    private String departmentName; // To display department name directly
    private LocalDate hireDate;
    private Double salary;
    private LocalDate dateOfBirth;
}