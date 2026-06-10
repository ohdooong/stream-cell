package com.streamcell.web.user.service.impl;

import com.streamcell.web.user.domain.User;
import com.streamcell.web.user.dto.UserResponse;
import com.streamcell.web.user.repository.UserMapper;
import com.streamcell.web.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return mapper.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    private UserResponse mapToDto(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .password(user.getPassword())
                .status(user.getStatus())
                .build();
    }
}

