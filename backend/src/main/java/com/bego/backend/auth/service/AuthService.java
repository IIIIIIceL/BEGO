package com.bego.backend.auth.service;

import com.bego.backend.auth.dto.LoginRequest;
import com.bego.backend.auth.dto.RefreshTokenRequest;
import com.bego.backend.auth.dto.RegisterRequest;
import com.bego.backend.auth.vo.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletRequest servletRequest);

    AuthResponse login(LoginRequest request, HttpServletRequest servletRequest);

    AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest);

    void logout(String authorizationHeader);

    String normalizeEmail(String email);
}
