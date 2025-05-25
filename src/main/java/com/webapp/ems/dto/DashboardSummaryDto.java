package com.webapp.ems.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private long totalEmployees;
    private long totalDepartments;
    private double averageEmployeeAge; // in years
    private List<AverageSalaryPerDepartmentDto> averageSalaryPerDepartment;
    private Map<String, Long> employeeCountByRole; // e.g., {"ADMIN": 5, "EMPLOYEE": 50}
    private Map<String, Long> employeeCountByDepartment; // Key: Department Name, Value: Count
    private Map<String, Long> employeeCountByGender;     // Key: Gender (e.g., "MALE", "FEMALE"), Value: Count
    private Map<String, Long> employeeCountByAgeGroup;   // Key: Age Group (e.g., "20-29", "30-39"), Value: Count
}