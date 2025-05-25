package com.webapp.ems.dto;

import com.webapp.ems.enums.Gender;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private Long id; // Null for creation, non-null for update/response
    private String firstName;
    private String lastName;
    private String email;
    private String password; // For creation or password change. Handle with care.
    private Gender gender;
    private Long managedByAdminId;
    private LocalDate dateOfBirth;
    private LocalDate hireDate;
    private Double salary;
    private String role; // Should default to "EMPLOYEE" for most employee operations
    private Long departmentId; // ID of the department
    private String departmentName; // For displaying department name (optional, can be populated by service)

    // Constructor without id and departmentName (useful for creation)
    public EmployeeDto(String firstName, String lastName, String email, String password, Gender gender,
                       LocalDate dateOfBirth, LocalDate hireDate, Double salary,
                       String role, Long departmentId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.hireDate = hireDate;
        this.salary = salary;
        this.role = role;
        this.departmentId = departmentId;
    }
}