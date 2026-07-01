package com.bego.backend.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record UpdateTodoRequest(
        @NotBlank @Size(min = 1, max = 120) String title,
        @Size(max = 5000) String description,
        @Pattern(regexp = "LOW|MEDIUM|HIGH") String priority,
        Instant dueAt,
        Instant reminderAt,
        Long sortOrder,
        List<Long> tagIds
) {
}
