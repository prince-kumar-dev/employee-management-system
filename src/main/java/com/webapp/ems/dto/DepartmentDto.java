package com.webapp.ems.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {
    private Long id; // Null for creation, non-null for update/response
    private String name;

    // Constructor without id (useful for creation)
    public DepartmentDto(String name) {
        this.name = name;
    }
}