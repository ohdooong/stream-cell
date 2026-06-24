package com.streamcell.web.user.service.impl;

import com.streamcell.web.user.converter.UserConverter;
import com.streamcell.web.user.domain.User;
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

    private final UserRepository mapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return mapper.findAll().stream()
                .map(UserConverter::toDTO)
                .toList();
    }
}

