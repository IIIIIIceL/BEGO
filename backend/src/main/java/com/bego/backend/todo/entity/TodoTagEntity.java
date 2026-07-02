package com.bego.backend.todo.entity;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodoTagEntity {
    private Long todoId;
    private Long tagId;
    private Long userId;
    private Instant createdAt;
}
