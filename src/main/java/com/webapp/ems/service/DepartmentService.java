package com.webapp.ems.service;

import com.webapp.ems.dto.DepartmentDto;
import com.webapp.ems.model.Department;
import com.webapp.ems.model.User; // Import User
import com.webapp.ems.enums.Role; // Import Role
import com.webapp.ems.repository.DepartmentRepository;
import com.webapp.ems.repository.UserRepository; // To fetch the admin user
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository; // To fetch admin User object

    private User getAdminUser(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Performing admin user not found with ID: " + adminId));
        if (admin.getRole() != Role.ADMIN) {
            throw new SecurityException("User performing action is not an Admin.");
        }
        return admin;
    }

    @Transactional
    public DepartmentDto createDepartment(DepartmentDto departmentDto, Long adminId) {
        User admin = getAdminUser(adminId);

        // Check for uniqueness within the admin's scope
        if (departmentRepository.existsByNameAndCreatedByAdmin(departmentDto.getName(), admin)) {
            throw new RuntimeException("Error: Department with name '" + departmentDto.getName() + "' already exists for this admin.");
        }

        Department department = new Department();
        department.setName(departmentDto.getName());
        department.setCreatedByAdmin(admin); // ***** SET THE OWNING ADMIN *****

        Department savedDepartment = departmentRepository.save(department);
        return mapToDto(savedDepartment);
    }

    public DepartmentDto getDepartmentById(Long id, Long adminId) {
        User admin = getAdminUser(adminId);
        Department department = departmentRepository.findByIdAndCreatedByAdmin(id, admin)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id + " for this admin."));
        return mapToDto(department);
    }

    public List<DepartmentDto> getAllDepartmentsByAdmin(Long adminId) {
        User admin = getAdminUser(adminId);
        return departmentRepository.findAllByCreatedByAdmin(admin).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentDto departmentDto, Long adminId) {
        User admin = getAdminUser(adminId);
        Department department = departmentRepository.findByIdAndCreatedByAdmin(id, admin) // Fetch department scoped to admin
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id + " for this admin."));

        // Check if the new name conflicts with another department OF THE SAME ADMIN
        if (!department.getName().equalsIgnoreCase(departmentDto.getName()) &&
                departmentRepository.existsByNameAndCreatedByAdmin(departmentDto.getName(), admin)) {
            throw new RuntimeException("Error: Another department with name '" + departmentDto.getName() + "' already exists for this admin.");
        }

        department.setName(departmentDto.getName());
        // createdByAdmin should not change during an update by the same admin
        Department updatedDepartment = departmentRepository.save(department);
        return mapToDto(updatedDepartment);
    }

    @Transactional
    public void deleteDepartment(Long id, Long adminId) {
        User admin = getAdminUser(adminId);
        Department department = departmentRepository.findByIdAndCreatedByAdmin(id, admin)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id + " or not managed by this admin."));

        if (!department.getEmployees().isEmpty()) {
            // Check if employees are specifically managed by THIS admin or if it's a global check
            // For now, assuming global check for simplicity of this example.
            // A more complex check: department.getEmployees().stream().anyMatch(e -> e.getManagedByAdmin().equals(admin))
            throw new RuntimeException("Cannot delete department. It has " + department.getEmployees().size() +
                    " associated employees. Please reassign or remove them first.");
        }
        departmentRepository.delete(department);
    }

    private DepartmentDto mapToDto(Department department) {
        // Include createdByAdminId in DTO if frontend needs it for some reason, though not typical for display
        return new DepartmentDto(department.getId(), department.getName() /*, department.getCreatedByAdmin().getId() */);
    }
    // Ensure DepartmentDto can handle the potential extra ID or remove it from constructor/setter if not needed

    // **** METHOD FOR PUBLIC LIST OF DEPARTMENTS (Scenario 1) ****
    public List<DepartmentDto> getAllPublicDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
}