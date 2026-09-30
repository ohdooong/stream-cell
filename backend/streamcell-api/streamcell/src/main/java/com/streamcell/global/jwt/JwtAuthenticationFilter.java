package com.streamcell.global.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        log.info("===== JwtAuthenticationFilter Start =====");

        final String authHeader = request.getHeader("Authorization");

        // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 다음 필터로 패스
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer " 이후의 실제 토큰 문자열 추출
        final String jwt = authHeader.substring(7);
        final String userLoginId;

        try {
            userLoginId = jwtTokenProvider.extractUsername(jwt);
            log.info("loginId: {}", userLoginId);
        } catch (JwtException e) {
            // 토큰 파싱 실패 시 인증 없이 다음 필터로 넘김 - 이후 인가 단계에서 거부됨
            log.warn("jwt 토큰 파싱 failed");
            filterChain.doFilter(request, response);
            return;
        }

        if (userLoginId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userLoginId);

            if (jwtTokenProvider.isTokenValid(jwt, userDetails)) {
                // 토큰이 유효하면 Authentication 객체 생성 후 SecurityContext에 등록
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null, // 이미 인증된 상태이므로 credentials은 null로 설정
                    userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        log.info("===== JwtAuthenticationFilter End =====");
        filterChain.doFilter(request, response);
    }
}
