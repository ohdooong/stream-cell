package com.streamcell.web.auth.service.impl;

import com.streamcell.global.jwt.JwtTokenProvider;
import com.streamcell.global.security.CustomUserDetails;
import com.streamcell.web.auth.dto.AuthRequest;
import com.streamcell.web.auth.dto.AuthResponse.Token;
import com.streamcell.web.auth.service.AuthService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Token login(AuthRequest.Login loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequest.getLoginId(),
                loginRequest.getPassword()
            )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateToken(
            Map.of("role", userDetails.getRole().name()),
            userDetails);


        return Token.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiration(jwtTokenProvider.getExpiration())
            .build();
    }

    @Override
    public void logout() {

    }
}
