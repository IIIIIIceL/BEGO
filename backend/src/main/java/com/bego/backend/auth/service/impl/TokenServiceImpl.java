package com.bego.backend.auth.service.impl;

import com.bego.backend.auth.entity.AuthTokenEntity;
import com.bego.backend.auth.mapper.AuthTokenMapper;
import com.bego.backend.auth.service.TokenService;
import com.bego.backend.auth.vo.TokenPair;
import com.bego.backend.common.time.TimeProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class TokenServiceImpl implements TokenService {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final AuthTokenMapper authTokenMapper;
    private final TimeProvider timeProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenServiceImpl(AuthTokenMapper authTokenMapper, TimeProvider timeProvider) {
        this.authTokenMapper = authTokenMapper;
        this.timeProvider = timeProvider;
    }

    @Override
    public TokenPair issueTokenPair(Long userId, String deviceName, String userAgent, String ipAddress) {
        Instant now = timeProvider.now();
        String refreshToken = generateToken();
        AuthTokenEntity refreshEntity = new AuthTokenEntity();
        refreshEntity.setUserId(userId);
        refreshEntity.setTokenHash(hash(refreshToken));
        refreshEntity.setTokenType("REFRESH");
        refreshEntity.setDeviceName(deviceName);
        refreshEntity.setUserAgent(userAgent);
        refreshEntity.setIpAddress(ipAddress);
        refreshEntity.setExpiresAt(now.plus(REFRESH_TOKEN_TTL));
        authTokenMapper.insert(refreshEntity);

        String accessToken = generateToken();
        AuthTokenEntity accessEntity = new AuthTokenEntity();
        accessEntity.setUserId(userId);
        accessEntity.setTokenHash(hash(accessToken));
        accessEntity.setTokenType("ACCESS");
        accessEntity.setParentTokenId(refreshEntity.getId());
        accessEntity.setDeviceName(deviceName);
        accessEntity.setUserAgent(userAgent);
        accessEntity.setIpAddress(ipAddress);
        accessEntity.setExpiresAt(now.plus(ACCESS_TOKEN_TTL));
        authTokenMapper.insert(accessEntity);

        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
