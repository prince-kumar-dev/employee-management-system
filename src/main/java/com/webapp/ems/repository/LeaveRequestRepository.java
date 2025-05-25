package com.webapp.ems.repository;

import com.webapp.ems.enums.LeaveStatus;
import com.webapp.ems.model.LeaveRequest;
import com.webapp.ems.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Import Optional

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployee(User employee);

    List<LeaveRequest> findByEmployeeId(Long employeeId); // For employee's own view

    // These are existing general queries, might be useful for a super-admin later,
    // but for scoped admin, we need new ones.
    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByEmployeeAndStatus(User employee, LeaveStatus status);


    // --- NEW QUERIES FOR ADMIN-SCOPED LEAVE MANAGEMENT ---

    // Find all leave requests for employees managed by a specific admin
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.managedByAdmin = :admin ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findAllByEmployee_ManagedByAdmin(@Param("admin") User admin);

    // Find leave requests by status for employees managed by a specific admin
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.managedByAdmin = :admin AND lr.status = :status ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findAllByEmployee_ManagedByAdminAndStatus(@Param("admin") User admin, @Param("status") LeaveStatus status);

    // Find a specific leave request by its ID, ensuring it belongs to an employee managed by the specific admin
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.id = :leaveRequestId AND lr.employee.managedByAdmin = :admin")
    Optional<LeaveRequest> findByIdAndEmployee_ManagedByAdmin(@Param("leaveRequestId") Long leaveRequestId, @Param("admin") User admin);
}