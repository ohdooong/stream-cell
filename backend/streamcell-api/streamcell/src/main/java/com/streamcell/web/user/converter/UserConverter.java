package com.streamcell.web.user.converter;

import com.streamcell.web.user.domain.User;
import com.streamcell.web.user.dto.UserResponse;

public class UserConverter {

    public static UserResponse toDTO(User user) {
        return UserResponse.builder()
            .userId(user.getUserId())
            .email(user.getEmail())
            .name(user.getName())
            .password(user.getPassword())
            .status(user.getStatus())
            .build();
    }
}
