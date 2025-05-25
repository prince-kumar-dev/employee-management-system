package com.webapp.ems.service;

import com.webapp.ems.dto.AverageSalaryPerDepartmentDto;
import com.webapp.ems.dto.DashboardSummaryDto;
import com.webapp.ems.enums.Gender;
import com.webapp.ems.enums.Role;
import com.webapp.ems.model.User; // Import User
import com.webapp.ems.repository.DepartmentRepository;
import com.webapp.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public DashboardSummaryDto getDashboardSummary(Long adminId) {
        User admin = userRepository.findById(adminId)
                .filter(u -> u.getRole() == Role.ADMIN) // Ensure the user is an admin
                .orElseThrow(() -> new RuntimeException("Admin user not found or user is not an admin: " + adminId));

        DashboardSummaryDto summary = new DashboardSummaryDto();

        // 1. Total Employees (managed by this admin)
        summary.setTotalEmployees(userRepository.countEmployeesManagedBy(admin));

        // 2. Total Departments (that have employees managed by this admin)
        summary.setTotalDepartments(departmentRepository.countDepartmentsWithEmployeesManagedBy(admin));
        // If departments are directly owned by admin:
        // summary.setTotalDepartments(departmentRepository.countByCreatedByAdmin(admin));


        // 3. Average Employee Age (for employees managed by this admin)
        Double avgAge = userRepository.findAverageAgeOfEmployeesManagedBy(admin);
        summary.setAverageEmployeeAge(avgAge != null ? avgAge : 0.0);

        // 4. Employee Count by Role (for users managed by this admin)
        Map<String, Long> countByRoleMap = new HashMap<>();
        List<Object[]> countsByRole = userRepository.countUsersByRoleManagedBy(admin);
        for (Object[] result : countsByRole) {
            Role roleEnum = (Role) result[0];
            Long count = (Long) result[1];
            countByRoleMap.put(roleEnum.name(), count);
        }
        summary.setEmployeeCountByRole(countByRoleMap);


        // 5. Average Salary per Department (for employees managed by this admin)
        List<AverageSalaryPerDepartmentDto> avgSalaries = userRepository.findAverageSalaryPerDepartmentForAdminEmployees(admin);
        summary.setAverageSalaryPerDepartment(avgSalaries);

        // 6. Employee Count by Department (for employees managed by this admin)
        Map<String, Long> employeeCountByDeptMap = new HashMap<>();
        List<Object[]> deptEmpCounts = userRepository.findDepartmentEmployeeCountsForAdmin(admin);
        for (Object[] result : deptEmpCounts) {
            String deptName = (String) result[0];
            Long count = (Long) result[1];
            employeeCountByDeptMap.put(deptName, count);
        }
        summary.setEmployeeCountByDepartment(employeeCountByDeptMap); // Ensure DTO has this field


        // 7. Employee Age Group Distribution (for employees managed by this admin)
        List<User> employeesForAgeCalc = userRepository.findEmployeesForAgeGroupCalculationManagedBy(admin);
        summary.setEmployeeCountByAgeGroup(calculateAgeGroupDistribution(employeesForAgeCalc));


        // Gender count (already scoped from previous implementation)
        Map<String, Long> genderCountsMap = new HashMap<>();
        List<Object[]> genderResults = userRepository.countEmployeesByGenderForAdmin(admin);
        for (Object[] result : genderResults) {
            Gender genderEnum = (Gender) result[0];
            Long count = (Long) result[1];
            genderCountsMap.put(genderEnum != null ? genderEnum.name() : "UNSPECIFIED", count);
        }
        summary.setEmployeeCountByGender(genderCountsMap);


        return summary;
    }

    // Helper method for age group calculation
    private Map<String, Long> calculateAgeGroupDistribution(List<User> employees) {
        Map<String, Long> ageGroupCounts = new HashMap<>();
        ageGroupCounts.put("Under 20", 0L);
        ageGroupCounts.put("20-29", 0L);
        ageGroupCounts.put("30-39", 0L);
        ageGroupCounts.put("40-49", 0L);
        ageGroupCounts.put("50-59", 0L);
        ageGroupCounts.put("60+", 0L);

        if (employees == null) return ageGroupCounts;

        LocalDate today = LocalDate.now();
        for (User employee : employees) {
            if (employee.getDateOfBirth() != null) {
                int age = Period.between(employee.getDateOfBirth(), today).getYears();
                if (age < 20) ageGroupCounts.merge("Under 20", 1L, Long::sum);
                else if (age <= 29) ageGroupCounts.merge("20-29", 1L, Long::sum);
                else if (age <= 39) ageGroupCounts.merge("30-39", 1L, Long::sum);
                else if (age <= 49) ageGroupCounts.merge("40-49", 1L, Long::sum);
                else if (age <= 59) ageGroupCounts.merge("50-59", 1L, Long::sum);
                else ageGroupCounts.merge("60+", 1L, Long::sum);
            }
        }
        return ageGroupCounts;
    }
}