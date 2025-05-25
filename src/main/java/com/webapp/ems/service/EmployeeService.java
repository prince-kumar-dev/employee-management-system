package com.webapp.ems.service;

import com.webapp.ems.dto.EmployeeDto;
import com.webapp.ems.enums.Gender;
import com.webapp.ems.enums.Role;
import com.webapp.ems.model.Department;
import com.webapp.ems.model.User;
import com.webapp.ems.repository.DepartmentRepository;
import com.webapp.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmailService emailService;

    // ADMIN CREATES EMPLOYEE (associates with self)
    @Transactional
    public EmployeeDto createEmployee(EmployeeDto employeeDto, Long adminId) { // Admin ID is now a parameter
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Performing admin user not found with ID: " + adminId));
        if (admin.getRole() != Role.ADMIN) {
            throw new SecurityException("User creating employee is not an Admin.");
        }

        // Global email check for simplicity, or scope it if desired
        if (userRepository.existsByEmail(employeeDto.getEmail())) {
            User existingUser = userRepository.findByEmail(employeeDto.getEmail()).orElse(null);
            if (existingUser != null && existingUser.isVerified()) {
                throw new RuntimeException("Error: Email is already in use by a verified user!");
            }
            // Handle unverified existing user if necessary (e.g. overwrite or error)
        }

        User employee = new User();
        mapDtoToEntity(employeeDto, employee, true); // isCreation = true

        employee.setRole(Role.EMPLOYEE); // Admin typically creates EMPLOYEEs
        employee.setVerified(true);
        employee.setManagedByAdmin(admin); // ***** KEY CHANGE: Link employee to the admin *****

        User savedEmployee = userRepository.save(employee);
        emailService.sendWelcomeEmail(savedEmployee);
        return mapEntityToDto(savedEmployee);
    }

    // ADMIN GETS THEIR EMPLOYEES
    public List<EmployeeDto> getAllEmployeesForAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));
        // Assuming you want to list only users with EMPLOYEE role managed by this admin
        return userRepository.findAllByRoleAndManagedByAdmin(Role.EMPLOYEE, admin).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    // ADMIN GETS A SPECIFIC EMPLOYEE THEY MANAGE
    public EmployeeDto getEmployeeByIdForAdmin(Long employeeId, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));
        User employee = userRepository.findByIdAndManagedByAdmin(employeeId, admin)
                .orElseThrow(() -> new RuntimeException("Employee not found or not managed by this admin."));
        if (employee.getRole() != Role.EMPLOYEE && employee.getManagedByAdmin() == null && employee.getId().equals(adminId)) {
            // Allow admin to view their own profile if getEmployeeById is also used for that
        } else if (employee.getManagedByAdmin() == null || !employee.getManagedByAdmin().getId().equals(adminId)) {
            throw new SecurityException("Admin not authorized to view this employee.");
        }
        return mapEntityToDto(employee);
    }

    // ADMIN UPDATES AN EMPLOYEE THEY MANAGE
    @Transactional
    public EmployeeDto updateEmployee(Long employeeId, EmployeeDto employeeDto, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));
        User employee = userRepository.findByIdAndManagedByAdmin(employeeId, admin)
                .orElseThrow(() -> new RuntimeException("Employee not found or not managed by this admin."));

        // Prevent changing managedByAdmin or making an employee an admin through this simple update
        if (employee.getManagedByAdmin() == null || !employee.getManagedByAdmin().getId().equals(adminId)) {
            throw new SecurityException("Admin not authorized to update this employee.");
        }

        boolean emailChanged = !employee.getEmail().equalsIgnoreCase(employeeDto.getEmail());
        if (emailChanged && userRepository.existsByEmail(employeeDto.getEmail())) {
            throw new RuntimeException("Error: New email is already in use by another user!");
        }

        mapDtoToEntity(employeeDto, employee, false); // isCreation = false
        // Ensure role remains EMPLOYEE or handle role changes carefully
        if (employeeDto.getRole() != null && Role.valueOf(employeeDto.getRole().toUpperCase()) == Role.EMPLOYEE){
            employee.setRole(Role.EMPLOYEE);
        } else if (employeeDto.getRole() != null) {
            // Potentially disallow changing to ADMIN here, or have a separate "promote" function
            System.err.println("Warning: Role change attempted in updateEmployee. Current role: " + employee.getRole());
        }


        User updatedEmployee = userRepository.save(employee);
        return mapEntityToDto(updatedEmployee);
    }

    // ADMIN DELETES AN EMPLOYEE THEY MANAGE
    @Transactional
    public void deleteEmployee(Long employeeId, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));
        User employee = userRepository.findByIdAndManagedByAdmin(employeeId, admin)
                .orElseThrow(() -> new RuntimeException("Employee not found or not managed by this admin."));
        if (employee.getManagedByAdmin() == null || !employee.getManagedByAdmin().getId().equals(adminId)) {
            throw new SecurityException("Admin not authorized to delete this employee.");
        }
        // Add more checks if needed, e.g., cannot delete self through this employee endpoint
        if (employee.getId().equals(adminId)) {
            throw new IllegalArgumentException("Admin cannot delete themselves using this employee function.");
        }

        userRepository.delete(employee);
    }

    // mapDtoToEntity and mapEntityToDto remain largely the same
    // but ensure they don't try to map/set managedByAdmin from the DTO directly
    // if that's not intended. managedByAdmin is set by the service logic.
    private void mapDtoToEntity(EmployeeDto dto, User user, boolean isCreation) {
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(dto.getPassword());
        } else if (isCreation) { // Password required for new employee creation by admin
            throw new RuntimeException("Password is required for new employee creation.");
        }

        user.setDateOfBirth(dto.getDateOfBirth());
        user.setHireDate(dto.getHireDate());
        user.setSalary(dto.getSalary());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found for ID: " + dto.getDepartmentId()));
            user.setDepartment(department);
            // TODO: If departments are also admin-scoped, ensure admin owns this department.
        } else {
            user.setDepartment(null);
        }

        if(dto.getGender() != null && !dto.getGender().toString().isEmpty()) {
            try {
                user.setGender(Gender.valueOf(dto.getGender().toString().toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid gender value provided: " + dto.getGender());
                user.setGender(null);
            }
        } else if (isCreation && dto.getRole().equals(Role.EMPLOYEE.name())) { // Assuming gender is mandatory for employee creation by admin
            user.setGender(null); // Or set a default like PREFER_NOT_TO_SAY
        }
    }
    private EmployeeDto mapEntityToDto(User user) {
        EmployeeDto dto = new EmployeeDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setHireDate(user.getHireDate());
        dto.setSalary(user.getSalary());
        dto.setRole(user.getRole().name());
        if (user.getDepartment() != null) {
            dto.setDepartmentId(user.getDepartment().getId());
            dto.setDepartmentName(user.getDepartment().getName());
        }
        if (user.getGender() != null) {
            dto.setGender(Gender.valueOf(user.getGender().name()));
        }
        // We don't typically send managedByAdmin_id back in EmployeeDto unless specifically needed by UI.
        return dto;
    }
}