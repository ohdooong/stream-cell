package com.streamcell.web.user.dto;

import com.streamcell.web.user.domain.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long userId;
    private String loginId;
    private String email;
    private String password;
    private String name;
    private String status;
    private Role role;
}
