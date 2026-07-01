package com.bego.backend.user.vo;

import com.bego.backend.user.entity.UserEntity;

public record UserProfileResponse(
        Long id,
        String email,
        String displayName,
        String avatarUrl
) {
    public static UserProfileResponse from(UserEntity user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }
}
