package com.bego.backend.common.security;

public record AuthenticatedUser(Long id, String email, String status) {
}
