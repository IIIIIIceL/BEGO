package com.bego.backend.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
        @NotBlank @Size(min = 1, max = 30) String name,
        String color,
        Integer sortOrder
) {
}
