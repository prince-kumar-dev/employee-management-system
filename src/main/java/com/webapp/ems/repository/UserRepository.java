package com.webapp.ems.repository;

import com.webapp.ems.dto.AverageSalaryPerDepartmentDto;
import com.webapp.ems.enums.Role;
import com.webapp.ems.model.Department;
import com.webapp.ems.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDepartment(Department department);

    List<User> findAllByRole(Role role);

    List<User> findAllByDepartmentIdAndRole(Long departmentId, Role role);

    List<User> findAllByRoleAndManagedByAdmin(Role role, User managedByAdmin);
    Optional<User> findByIdAndManagedByAdmin(Long id, User managedByAdmin);
    long countByRoleAndManagedByAdmin(Role role, User managedByAdmin);

    // For Dashboard: Count total employees (users with EMPLOYEE role)
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE")
    long countByRoleEmployee();

    // For Dashboard: Average age of employees.
    // Note: JPQL date functions can be limited. A native query might be more robust for complex date arithmetic.
    // This simplified query calculates average age based on years, good for an example.
    // For a precise age, you'd need more complex logic or a native query.
    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(u.dateOfBirth)) " +
            "FROM User u " +
            "WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.dateOfBirth IS NOT NULL")
    Double findAverageAgeOfEmployees();

    // For Dashboard: Average salary in each department for EMPLOYEES
    // Uses a DTO projection. The DTO must have a constructor matching the SELECT clause.
    // com.webapp.ems.dto.AverageSalaryPerDepartmentDto(String departmentName, double averageSalary)
    @Query("SELECT new com.webapp.ems.dto.AverageSalaryPerDepartmentDto(d.name, AVG(u.salary)) " +
            "FROM User u JOIN u.department d " +
            "WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.salary IS NOT NULL " +
            "GROUP BY d.id, d.name")
    List<AverageSalaryPerDepartmentDto> findAverageSalaryPerDepartmentForEmployees();


    // For Dashboard: Count of users by role (e.g., how many ADMINS, how many EMPLOYEES)
    // Returns a list of Object arrays: [Role roleEnum, Long count]
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countUsersByRole();

    // For Dashboard: Employee count by department
    // Returns List of Object arrays: [String departmentName, Long count]
    @Query("SELECT d.name, COUNT(u.id) FROM User u JOIN u.department d WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE GROUP BY d.id, d.name")
    List<Object[]> countEmployeesByDepartment();

    // For Dashboard: Employee count by gender
    // Returns List of Object arrays: [User.Gender genderEnum, Long count]
    // Make sure User.Gender is your enum type
    @Query("SELECT u.gender, COUNT(u.id) FROM User u WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.gender IS NOT NULL GROUP BY u.gender")
    List<Object[]> countEmployeesByGender();

    // For Dashboard: Fetch all employee birth dates (for age group calculation in service)
    @Query("SELECT u.dateOfBirth FROM User u WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.dateOfBirth IS NOT NULL")
    List<java.time.LocalDate> findAllEmployeeBirthDates();

    @Query("SELECT u.gender, COUNT(u) FROM User u WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.managedByAdmin = :admin GROUP BY u.gender")
    List<Object[]> countEmployeesByGenderForAdmin(@Param("admin") User admin);

    // 1. Count total employees managed by a specific admin
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.managedByAdmin = :admin")
    long countEmployeesManagedBy(@Param("admin") User admin);

    // 3. Average age of employees managed by a specific admin
    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(u.dateOfBirth)) " +
            "FROM User u " +
            "WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.managedByAdmin = :admin AND u.dateOfBirth IS NOT NULL")
    Double findAverageAgeOfEmployeesManagedBy(@Param("admin") User admin);


    // 4. Count of users by role (EMPLOYEE, potentially others if admin creates other types) managed by this admin
    //    If an admin can only create EMPLOYEEs under them, this might simplify to just employee count.
    //    If an admin can create other roles under them (e.g. team leads also as User objects), this query is useful.
    @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.managedByAdmin = :admin GROUP BY u.role")
    List<Object[]> countUsersByRoleManagedBy(@Param("admin") User admin);


    // 5. Average salary in each department FOR EMPLOYEES MANAGED BY THIS ADMIN
    //    The DTO projection needs to be adjusted if the DTO is global.
    //    This query assumes an employee is in a department AND managed by this admin.
    @Query("SELECT new com.webapp.ems.dto.AverageSalaryPerDepartmentDto(d.name, AVG(u.salary)) " +
            "FROM User u JOIN u.department d " +
            "WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.managedByAdmin = :admin AND u.salary IS NOT NULL " +
            "GROUP BY d.id, d.name")
    List<AverageSalaryPerDepartmentDto> findAverageSalaryPerDepartmentForAdminEmployees(@Param("admin") User admin);

    // 6. Employee count by department FOR EMPLOYEES MANAGED BY THIS ADMIN
    // Returns [String departmentName, Long employeeCount]
    @Query("SELECT d.name, COUNT(u.id) FROM User u JOIN u.department d " +
            "WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.managedByAdmin = :admin " +
            "GROUP BY d.id, d.name")
    List<Object[]> findDepartmentEmployeeCountsForAdmin(@Param("admin") User admin);


    // 7. Employee Age Group Distribution FOR EMPLOYEES MANAGED BY THIS ADMIN
    //    This is more complex for a single JPQL query. Usually done in service layer after fetching relevant users.
    //    For now, let's assume DashboardService will fetch employees managed by admin and then calculate age groups.
    //    If you need a direct DB query, it would be database-specific or a series of CASE WHEN statements.
    //    Alternatively, fetch all employees managed by admin and process in Java.
    @Query("SELECT u FROM User u WHERE u.role = com.webapp.ems.enums.Role.EMPLOYEE AND u.managedByAdmin = :admin AND u.dateOfBirth IS NOT NULL")
    List<User> findEmployeesForAgeGroupCalculationManagedBy(@Param("admin") User admin);

    long countByDepartmentAndManagedByAdmin(Department department, User managedByAdmin);
}