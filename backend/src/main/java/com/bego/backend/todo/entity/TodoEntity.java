package com.bego.backend.todo.entity;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodoEntity {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String clientTempId;
    private String syncStatus;
    private Instant lastSyncedAt;
    private Instant dueAt;
    private Instant reminderAt;
    private Instant completedAt;
    private Long sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
