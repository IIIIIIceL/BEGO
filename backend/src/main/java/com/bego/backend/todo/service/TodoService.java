package com.bego.backend.todo.service;

import com.bego.backend.common.response.PageResponse;
import com.bego.backend.todo.dto.CreateTodoRequest;
import com.bego.backend.todo.dto.UpdateTodoRequest;
import com.bego.backend.todo.vo.TodoResponse;
import java.time.Instant;

public interface TodoService {
    TodoResponse create(Long userId, CreateTodoRequest request);

    TodoResponse get(Long userId, Long todoId);

    PageResponse<TodoResponse> list(
            Long userId,
            String status,
            String priority,
            Long tagId,
            String keyword,
            Instant dueFrom,
            Instant dueTo,
            String sort,
            Integer page,
            Integer size
    );

    TodoResponse update(Long userId, Long todoId, UpdateTodoRequest request);

    TodoResponse updateStatus(Long userId, Long todoId, String status);

    void delete(Long userId, Long todoId);
}
