package com.streamcell.web.user.service;

import com.streamcell.web.user.domain.User;
import com.streamcell.web.user.dto.UserResponse;

import java.util.List;

/**
 *
 */
public interface UserService {
    List<UserResponse> findAll();

    UserResponse findByUserId(Long userId);

    List<UserResponse> findByUserName(String userName);


}
