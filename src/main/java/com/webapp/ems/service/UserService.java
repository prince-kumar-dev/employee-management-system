package com.webapp.ems.service;

import com.webapp.ems.dto.UserSimpleDto;
import com.webapp.ems.enums.Role;
import com.webapp.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserSimpleDto> getAllAdminsSimple() {
        return userRepository.findAllByRole(Role.ADMIN).stream()
                .map(admin -> new UserSimpleDto(admin.getId(), admin.getFirstName() + " " + admin.getLastName()))
                .collect(Collectors.toList());
    }
}