package com.bego.backend.common.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String code,
        String message,
        List<ValidationErrorDetail> details,
        String path,
        Instant timestamp
) {
    public static ApiError of(ErrorCode errorCode, String message, String path) {
        return new ApiError(errorCode.name(), message, List.of(), path, Instant.now());
    }

    public static ApiError validation(List<ValidationErrorDetail> details, String path) {
        return new ApiError(
                ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                details,
                path,
                Instant.now()
        );
    }
}
