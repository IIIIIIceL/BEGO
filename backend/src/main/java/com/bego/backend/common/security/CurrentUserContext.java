package com.bego.backend.common.security;

import com.bego.backend.common.error.ApiException;
import com.bego.backend.common.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserContext {
    private CurrentUserContext() {
    }

    public static Long requireUserId() {
        return requireUser().id();
    }

    public static AuthenticatedUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED);
    }
}
