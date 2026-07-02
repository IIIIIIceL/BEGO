package com.bego.backend.auth.entity;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthTokenEntity {
    private Long id;
    private Long userId;
    private String tokenHash;
    private String tokenType;
    private Long parentTokenId;
    private String deviceName;
    private String userAgent;
    private String ipAddress;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private Instant revokedAt;
    private Instant createdAt;
}
