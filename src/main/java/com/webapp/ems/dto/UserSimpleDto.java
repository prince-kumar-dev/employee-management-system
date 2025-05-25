package com.webapp.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleDto {
    private Long id;
    private String fullName; // e.g., "AdminFirstName AdminLastName"
}