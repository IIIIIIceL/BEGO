package com.bego.backend.auth.service;

import com.bego.backend.auth.vo.TokenPair;

public interface TokenService {
    TokenPair issueTokenPair(Long userId, String deviceName, String userAgent, String ipAddress);

    String hash(String token);
}
