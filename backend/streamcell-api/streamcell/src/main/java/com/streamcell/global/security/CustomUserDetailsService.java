package com.streamcell.global.security;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByLoginId(username)
            .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_USER));
    }
}
