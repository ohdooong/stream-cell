package com.streamcell.web.user.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.web.user.converter.UserConverter;
import com.streamcell.web.user.dto.UserResponse;
import com.streamcell.web.user.repository.UserRepository;
import com.streamcell.web.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream()
                .map(UserConverter::toDTO)
                .toList();
    }

    @Override
    public UserResponse findByUserId(Long userId) {
        return repository.findById(userId)
            .map(UserConverter::toDTO)
            .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_USER));
    }

    @Override
    public List<UserResponse> findByUserName(String userName) {
        return repository.findByUserName(userName)
            .stream()
            .map(UserConverter::toDTO)
            .toList();
    }
}

