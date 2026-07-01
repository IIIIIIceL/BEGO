package com.bego.backend.tag.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record CreateTagRequest(
        @NotBlank @Size(min = 1, max = 30) String name,
        String color,
        Integer sortOrder
) {
}
