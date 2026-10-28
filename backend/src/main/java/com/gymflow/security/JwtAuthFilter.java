package com.gymflow.security;

import com.gymflow.domain.UserStatus;
import com.gymflow.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Đọc Bearer token, nạp lại user từ CSDL ở mỗi request:
 * tài khoản bị khoá hoặc đổi vai trò có hiệu lực ngay, kể cả với token đã cấp trước đó.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.parseUserId(header.substring(7).trim())
                    .flatMap(userRepository::findById)
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .ifPresent(u -> {
                        AuthUser principal = new AuthUser(u.getId(), u.getEmail(), u.getRole(), u.getFullName());
                        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }
}
