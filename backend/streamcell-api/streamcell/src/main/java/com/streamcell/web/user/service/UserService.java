package com.streamcell.web.user.service;

import com.streamcell.web.user.dto.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> findAll();
}
