package com.bego.backend.auth.vo;

import com.bego.backend.user.vo.UserProfileResponse;

public record AuthResponse(
        UserProfileResponse user,
        String accessToken,
        String refreshToken
) {
}
