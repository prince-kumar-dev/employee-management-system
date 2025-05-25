package com.webapp.ems.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AverageSalaryPerDepartmentDto {
    private String departmentName;
    private double averageSalary;
}