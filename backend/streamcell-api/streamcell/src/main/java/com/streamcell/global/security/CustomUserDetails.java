package com.streamcell.global.security;

import com.streamcell.web.user.domain.Role;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@AllArgsConstructor
@Builder
public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String loginId;
    private String name;
    private String email;
    private String encryptedPassword;
    private String status;
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return this.encryptedPassword;
    }

    @Override
    public String getUsername() {
        return this.loginId;
    }
}
