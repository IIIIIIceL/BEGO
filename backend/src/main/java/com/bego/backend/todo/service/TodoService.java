package com.bego.backend.todo.service;

import com.bego.backend.common.error.ApiException;
import com.bego.backend.common.error.ErrorCode;
import com.bego.backend.common.response.PageResponse;
import com.bego.backend.common.time.TimeProvider;
import com.bego.backend.tag.mapper.TagMapper;
import com.bego.backend.todo.dto.CreateTodoRequest;
import com.bego.backend.todo.dto.UpdateTodoRequest;
import com.bego.backend.todo.entity.TodoEntity;
import com.bego.backend.todo.entity.TodoTagEntity;
import com.bego.backend.todo.mapper.TodoMapper;
import com.bego.backend.todo.mapper.TodoTagMapper;
import com.bego.backend.todo.vo.TodoResponse;
import com.bego.backend.todo.vo.TodoTagResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> STATUSES = Set.of("TODO", "DONE");
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> SORTS = Set.of("DEFAULT", "UPDATED_DESC", "CREATED_DESC", "DUE_ASC");

    private final TodoMapper todoMapper;
    private final TodoTagMapper todoTagMapper;
    private final TagMapper tagMapper;
    private final TimeProvider timeProvider;

    public TodoService(
            TodoMapper todoMapper,
            TodoTagMapper todoTagMapper,
            TagMapper tagMapper,
            TimeProvider timeProvider
    ) {
        this.todoMapper = todoMapper;
        this.todoTagMapper = todoTagMapper;
        this.tagMapper = tagMapper;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public TodoResponse create(Long userId, CreateTodoRequest request) {
        String clientTempId = cleanNullable(request.clientTempId());
        if (clientTempId != null) {
            TodoEntity existing = todoMapper.findActiveByUserIdAndClientTempId(userId, clientTempId);
            if (existing != null) {
                return toResponse(userId, existing);
            }
        }

        List<Long> tagIds = validateAndNormalizeTagIds(userId, request.tagIds());

        TodoEntity todo = new TodoEntity();
        todo.setUserId(userId);
        todo.setTitle(request.title().trim());
        todo.setDescription(cleanNullable(request.description()));
        todo.setStatus("TODO");
        todo.setPriority(defaultPriority(request.priority()));
        todo.setClientTempId(clientTempId);
        todo.setSyncStatus("SYNCED");
        todo.setLastSyncedAt(timeProvider.now());
        todo.setDueAt(request.dueAt());
        todo.setReminderAt(request.reminderAt());
        todo.setSortOrder(request.sortOrder() == null ? 0L : request.sortOrder());

        try {
            todoMapper.insert(todo);
        } catch (DuplicateKeyException exception) {
            TodoEntity existing = clientTempId == null
                    ? null
                    : todoMapper.findActiveByUserIdAndClientTempId(userId, clientTempId);
            if (existing != null) {
                return toResponse(userId, existing);
            }
            throw new ApiException(ErrorCode.CLIENT_TEMP_ID_CONFLICT);
        }

        replaceTags(userId, todo.getId(), tagIds);
        return get(userId, todo.getId());
    }

    public TodoResponse get(Long userId, Long todoId) {
        return toResponse(userId, getRequiredActiveTodo(userId, todoId));
    }

    public PageResponse<TodoResponse> list(
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
    ) {
        String cleanStatus = cleanStatus(status);
        String cleanPriority = cleanPriority(priority);
        String cleanSort = cleanSort(sort);
        validateTagFilter(userId, tagId);
        int cleanPage = page == null ? DEFAULT_PAGE : page;
        int cleanSize = size == null ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        if (cleanPage < 0 || cleanSize <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Page must be >= 0 and size must be > 0");
        }
        if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "dueFrom must be before dueTo");
        }

        String keywordLike = keywordLike(keyword);
        int offset = cleanPage * cleanSize;
        List<TodoEntity> todos = todoMapper.searchActivePage(
                userId,
                cleanStatus,
                cleanPriority,
                tagId,
                keywordLike,
                dueFrom,
                dueTo,
                cleanSort,
                cleanSize,
                offset
        );
        long total = todoMapper.countSearchActive(
                userId,
                cleanStatus,
                cleanPriority,
                tagId,
                keywordLike,
                dueFrom,
                dueTo
        );
        List<TodoResponse> items = todos.stream()
                .map(todo -> toResponse(userId, todo))
                .toList();
        return new PageResponse<>(items, cleanPage, cleanSize, total);
    }

    @Transactional
    public TodoResponse update(Long userId, Long todoId, UpdateTodoRequest request) {
        TodoEntity existing = getRequiredActiveTodo(userId, todoId);
        List<Long> tagIds = validateAndNormalizeTagIds(userId, request.tagIds());

        existing.setTitle(request.title().trim());
        existing.setDescription(cleanNullable(request.description()));
        existing.setPriority(defaultPriority(request.priority()));
        existing.setDueAt(request.dueAt());
        existing.setReminderAt(request.reminderAt());
        existing.setSortOrder(request.sortOrder() == null ? existing.getSortOrder() : request.sortOrder());

        int updated = todoMapper.update(existing);
        if (updated == 0) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        replaceTags(userId, todoId, tagIds);
        return get(userId, todoId);
    }

    @Transactional
    public TodoResponse updateStatus(Long userId, Long todoId, String status) {
        String cleanStatus = cleanStatus(status);
        Instant completedAt = "DONE".equals(cleanStatus) ? timeProvider.now() : null;
        int updated = todoMapper.updateStatus(todoId, userId, cleanStatus, completedAt);
        if (updated == 0) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return get(userId, todoId);
    }

    @Transactional
    public void delete(Long userId, Long todoId) {
        getRequiredActiveTodo(userId, todoId);
        todoTagMapper.deleteByTodoIdAndUserId(todoId, userId);
        int deleted = todoMapper.softDeleteByIdAndUserId(todoId, userId);
        if (deleted == 0) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private TodoEntity getRequiredActiveTodo(Long userId, Long todoId) {
        TodoEntity todo = todoMapper.findActiveByIdAndUserId(todoId, userId);
        if (todo == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return todo;
    }

    private TodoResponse toResponse(Long userId, TodoEntity todo) {
        List<TodoTagResponse> tags = todoTagMapper.findTagResponsesByTodoIdAndUserId(todo.getId(), userId);
        return TodoResponse.from(todo, tags);
    }

    private void replaceTags(Long userId, Long todoId, List<Long> tagIds) {
        todoTagMapper.deleteByTodoIdAndUserId(todoId, userId);
        for (Long tagId : tagIds) {
            TodoTagEntity relation = new TodoTagEntity();
            relation.setUserId(userId);
            relation.setTodoId(todoId);
            relation.setTagId(tagId);
            todoTagMapper.insert(relation);
        }
    }

    private List<Long> validateAndNormalizeTagIds(Long userId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalized = new ArrayList<>(new LinkedHashSet<>(tagIds));
        if (normalized.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tag ids must be positive");
        }
        int existingCount = tagMapper.countActiveByUserIdAndIds(userId, normalized);
        if (existingCount != normalized.size()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return normalized;
    }

    private void validateTagFilter(Long userId, Long tagId) {
        if (tagId == null) {
            return;
        }
        if (tagMapper.findActiveByIdAndUserId(tagId, userId) == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private String cleanStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String cleanStatus = status.trim().toUpperCase();
        if (!STATUSES.contains(cleanStatus)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Todo status is invalid");
        }
        return cleanStatus;
    }

    private String cleanPriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        String cleanPriority = priority.trim().toUpperCase();
        if (!PRIORITIES.contains(cleanPriority)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Todo priority is invalid");
        }
        return cleanPriority;
    }

    private String defaultPriority(String priority) {
        String cleanPriority = cleanPriority(priority);
        return cleanPriority == null ? "MEDIUM" : cleanPriority;
    }

    private String cleanSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "DEFAULT";
        }
        String cleanSort = sort.trim().toUpperCase();
        if (!SORTS.contains(cleanSort)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Todo sort is invalid");
        }
        return cleanSort;
    }

    private String cleanNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String keywordLike(String keyword) {
        String cleanKeyword = cleanNullable(keyword);
        return cleanKeyword == null ? null : "%" + cleanKeyword + "%";
    }
}
