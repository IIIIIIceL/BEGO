package com.bego.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteMeRequest(@NotBlank String password) {
}
