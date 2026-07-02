package com.bego.backend.user.entity;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserEntity {
    private Long id;
    private String email;
    private String normalizedEmail;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private String status;
    private Instant lastLoginAt;
    private Instant deletionRequestedAt;
    private Instant anonymizedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
