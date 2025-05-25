package com.webapp.ems.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "created_by_admin_id"}, name = "uk_department_name_per_admin")
        } // Defines that the combination of name and created_by_admin_id must be unique
)
@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: Generates no-args constructor
@AllArgsConstructor // Lombok: Generates all-args constructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<User> employees = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id", nullable = false)
    private User createdByAdmin;

    // Helper method to add an employee to the department
    public void addEmployee(User employee) {
        employees.add(employee);
        employee.setDepartment(this);
    }

    // Helper method to remove an employee from the department
    public void removeEmployee(User employee) {
        employees.remove(employee);
        employee.setDepartment(null);
    }
}