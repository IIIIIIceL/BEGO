package com.bego.backend.auth.security;

import com.bego.backend.auth.entity.AuthTokenEntity;
import com.bego.backend.auth.mapper.AuthTokenMapper;
import com.bego.backend.auth.service.TokenService;
import com.bego.backend.common.security.AuthenticatedUser;
import com.bego.backend.common.time.TimeProvider;
import com.bego.backend.user.entity.UserEntity;
import com.bego.backend.user.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {
    private final AuthTokenMapper authTokenMapper;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final TimeProvider timeProvider;

    public BearerTokenAuthenticationFilter(
            AuthTokenMapper authTokenMapper,
            UserMapper userMapper,
            TokenService tokenService,
            TimeProvider timeProvider
    ) {
        this.authTokenMapper = authTokenMapper;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.timeProvider = timeProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request.getHeader("Authorization"));
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        AuthTokenEntity accessToken = authTokenMapper.findUsableAccessTokenByHash(tokenService.hash(token), timeProvider.now());
        if (accessToken == null) {
            return;
        }
        UserEntity user = userMapper.findActiveById(accessToken.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            return;
        }

        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getStatus());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
