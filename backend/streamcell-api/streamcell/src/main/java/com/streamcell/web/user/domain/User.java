package com.streamcell.web.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class User {

    private Long userId;
    private String loginId;
    private String name;
    private String email;
    private String encryptedPassword;
    private String status;
    private Role role;

}
