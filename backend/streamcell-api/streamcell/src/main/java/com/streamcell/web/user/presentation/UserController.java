package com.streamcell.web.user.presentation;

import com.streamcell.web.user.dto.UserResponse;
import com.streamcell.web.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/web/user")
public class UserController {

    private final UserService service;

    @GetMapping("/items")
    public ResponseEntity<List<UserResponse>> selectUserList() {
        return ResponseEntity.ok(service.findAll());
    }
}
