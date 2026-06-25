package com.streamcell.web.user.domain;


import lombok.Getter;
import lombok.Setter;


@Getter
public class User {
    private String userId;
    private String name;
    private String email;
    private String password;
    private String status;
}
