package com.bego.backend.auth.service.impl;

import com.bego.backend.auth.dto.LoginRequest;
import com.bego.backend.auth.dto.RefreshTokenRequest;
import com.bego.backend.auth.dto.RegisterRequest;
import com.bego.backend.auth.entity.AuthTokenEntity;
import com.bego.backend.auth.mapper.AuthTokenMapper;
import com.bego.backend.auth.service.AuthService;
import com.bego.backend.auth.service.TokenService;
import com.bego.backend.auth.vo.AuthResponse;
import com.bego.backend.auth.vo.TokenPair;
import com.bego.backend.common.error.ApiException;
import com.bego.backend.common.error.ErrorCode;
import com.bego.backend.common.time.TimeProvider;
import com.bego.backend.user.entity.UserEntity;
import com.bego.backend.user.mapper.UserMapper;
import com.bego.backend.user.vo.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final AuthTokenMapper authTokenMapper;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    public AuthServiceImpl(
            UserMapper userMapper,
            AuthTokenMapper authTokenMapper,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            TimeProvider timeProvider
    ) {
        this.userMapper = userMapper;
        this.authTokenMapper = authTokenMapper;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = timeProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest servletRequest) {
        UserEntity user = new UserEntity();
        user.setEmail(request.email().trim());
        user.setNormalizedEmail(normalizeEmail(request.email()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setStatus("ACTIVE");

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        TokenPair tokenPair = tokenService.issueTokenPair(
                user.getId(),
                null,
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr()
        );
        return new AuthResponse(UserProfileResponse.from(user), tokenPair.accessToken(), tokenPair.refreshToken());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        UserEntity user = userMapper.findActiveByNormalizedEmail(normalizeEmail(request.email()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }
        assertLoginAllowed(user);

        userMapper.updateLastLoginAt(user.getId(), timeProvider.now());
        TokenPair tokenPair = tokenService.issueTokenPair(
                user.getId(),
                request.deviceName(),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr()
        );
        return new AuthResponse(UserProfileResponse.from(user), tokenPair.accessToken(), tokenPair.refreshToken());
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        AuthTokenEntity refreshToken = authTokenMapper.findUsableRefreshTokenByHash(
                tokenService.hash(request.refreshToken()),
                timeProvider.now()
        );
        if (refreshToken == null) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        UserEntity user = userMapper.findActiveById(refreshToken.getUserId());
        if (user == null) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        assertLoginAllowed(user);

        authTokenMapper.revokeById(refreshToken.getId(), timeProvider.now());
        TokenPair tokenPair = tokenService.issueTokenPair(
                user.getId(),
                refreshToken.getDeviceName(),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr()
        );
        return new AuthResponse(UserProfileResponse.from(user), tokenPair.accessToken(), tokenPair.refreshToken());
    }

    @Override
    @Transactional
    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            return;
        }
        AuthTokenEntity accessToken = authTokenMapper.findUsableAccessTokenByHash(tokenService.hash(token), timeProvider.now());
        if (accessToken == null) {
            return;
        }
        authTokenMapper.revokeAccessTokenAndParentFamily(
                accessToken.getId(),
                accessToken.getParentTokenId(),
                timeProvider.now()
        );
    }

    @Override
    public String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void assertLoginAllowed(UserEntity user) {
        if ("DISABLED".equals(user.getStatus())) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        if ("DELETED".equals(user.getStatus())) {
            throw new ApiException(ErrorCode.ACCOUNT_DELETED);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
