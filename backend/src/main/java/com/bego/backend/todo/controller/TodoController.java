package com.bego.backend.todo.controller;

import com.bego.backend.common.response.PageResponse;
import com.bego.backend.common.security.CurrentUserContext;
import com.bego.backend.todo.dto.CreateTodoRequest;
import com.bego.backend.todo.dto.UpdateTodoRequest;
import com.bego.backend.todo.dto.UpdateTodoStatusRequest;
import com.bego.backend.todo.service.TodoService;
import com.bego.backend.todo.vo.TodoResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {
    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public PageResponse<TodoResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Instant dueFrom,
            @RequestParam(required = false) Instant dueTo,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return todoService.list(
                CurrentUserContext.requireUserId(),
                status,
                priority,
                tagId,
                keyword,
                dueFrom,
                dueTo,
                sort,
                page,
                size
        );
    }

    @PostMapping
    public TodoResponse create(@Valid @RequestBody CreateTodoRequest request) {
        return todoService.create(CurrentUserContext.requireUserId(), request);
    }

    @GetMapping("/{id}")
    public TodoResponse get(@PathVariable Long id) {
        return todoService.get(CurrentUserContext.requireUserId(), id);
    }

    @PutMapping("/{id}")
    public TodoResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request
    ) {
        return todoService.update(CurrentUserContext.requireUserId(), id, request);
    }

    @PatchMapping("/{id}/status")
    public TodoResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoStatusRequest request
    ) {
        return todoService.updateStatus(CurrentUserContext.requireUserId(), id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        todoService.delete(CurrentUserContext.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
