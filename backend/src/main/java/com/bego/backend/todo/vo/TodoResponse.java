package com.bego.backend.todo.vo;

import com.bego.backend.todo.entity.TodoEntity;
import java.time.Instant;
import java.util.List;

public record TodoResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        String clientTempId,
        String syncStatus,
        Instant dueAt,
        Instant reminderAt,
        Instant completedAt,
        Long sortOrder,
        Instant createdAt,
        Instant updatedAt,
        List<TodoTagResponse> tags
) {
    public static TodoResponse from(TodoEntity todo, List<TodoTagResponse> tags) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.getStatus(),
                todo.getPriority(),
                todo.getClientTempId(),
                todo.getSyncStatus(),
                todo.getDueAt(),
                todo.getReminderAt(),
                todo.getCompletedAt(),
                todo.getSortOrder(),
                todo.getCreatedAt(),
                todo.getUpdatedAt(),
                tags
        );
    }
}
