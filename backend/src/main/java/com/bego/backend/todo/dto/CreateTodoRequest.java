package com.bego.backend.todo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public record CreateTodoRequest(
        @NotBlank @Size(min = 1, max = 120) String title,
        @Size(max = 5000) String description,
        @Pattern(regexp = "LOW|MEDIUM|HIGH") String priority,
        Instant dueAt,
        Instant reminderAt,
        Long sortOrder,
        @Size(max = 64) String clientTempId,
        List<Long> tagIds
) {
}
