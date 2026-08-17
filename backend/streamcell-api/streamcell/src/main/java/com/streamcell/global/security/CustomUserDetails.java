package com.streamcell.global.security;

import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private String userId;
    private String loginId;
    private String email;
    private String name;
    private String encryptedPassword;
    private String status;
    List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return this.encryptedPassword;
    }

    @Override
    public String getUsername() {
        return "";
    }
}
