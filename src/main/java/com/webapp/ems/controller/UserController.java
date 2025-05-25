package com.webapp.ems.controller;

import com.webapp.ems.dto.UserSimpleDto;
import com.webapp.ems.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController { // Or rename if it's just for this specific task

    private final UserService userService; // Or directly UserRepository for this simple case

    @GetMapping("/api/admins/list") // New endpoint
    public ResponseEntity<List<UserSimpleDto>> getAdminList() {
        List<UserSimpleDto> admins = userService.getAllAdminsSimple();
        return ResponseEntity.ok(admins);
    }
    // ... other user-related endpoints if any ...
}