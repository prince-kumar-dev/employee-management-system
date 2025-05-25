package com.webapp.ems.repository;

import com.webapp.ems.model.Department;
import com.webapp.ems.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByCreatedByAdmin(User createdByAdmin);
    boolean existsByName(String name); // Global check

    // Find by name SCOPED to an admin
    Optional<Department> findByNameAndCreatedByAdmin(String name, User createdByAdmin);
    // Check existence by name SCOPED to an admin
    boolean existsByNameAndCreatedByAdmin(String name, User createdByAdmin);

    // Find all departments created by a specific admin
    List<Department> findAllByCreatedByAdmin(User createdByAdmin);

    // Find a specific department by its ID and ensuring it was created by the specific admin
    Optional<Department> findByIdAndCreatedByAdmin(Long id, User createdByAdmin);

    // Your existing query, now needs to be considered in context of admin scoping for dashboard
    // This original query might need to be adapted or a new one created if dashboard stats are also admin-scoped
    @Query("SELECT d.name, COUNT(e.id) FROM Department d LEFT JOIN d.employees e WHERE e.role = com.webapp.ems.enums.Role.EMPLOYEE GROUP BY d.id, d.name")
    List<Object[]> findDepartmentEmployeeCountsGlobal(); // Kept original for now, might need scoping

    // Example of a scoped department employee count (if dashboard is admin-specific)
    @Query("SELECT d.name, COUNT(e.id) FROM Department d LEFT JOIN d.employees e WHERE d.createdByAdmin = :admin AND e.role = com.webapp.ems.enums.Role.EMPLOYEE GROUP BY d.id, d.name")
    List<Object[]> findDepartmentEmployeeCountsForAdmin(User admin);

    // For Dashboard: Count distinct departments that have employees managed by a specific admin
    @Query("SELECT COUNT(DISTINCT d.id) FROM Department d JOIN d.employees e WHERE e.managedByAdmin = :admin AND e.role = com.webapp.ems.enums.Role.EMPLOYEE")
    long countDepartmentsWithEmployeesManagedBy(@Param("admin") User admin);
}