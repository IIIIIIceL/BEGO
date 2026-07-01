package com.bego.backend.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request parameters are invalid"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token has expired"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token is invalid"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account is disabled"),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "Account has been deleted"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource was not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    TAG_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tag name already exists"),
    CLIENT_TEMP_ID_CONFLICT(HttpStatus.CONFLICT, "Client temp id conflicts with existing data"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
