package com.bego.backend.tag.service;

import com.bego.backend.common.error.ApiException;
import com.bego.backend.common.error.ErrorCode;
import com.bego.backend.tag.dto.CreateTagRequest;
import com.bego.backend.tag.dto.ReorderTagItem;
import com.bego.backend.tag.dto.ReorderTagsRequest;
import com.bego.backend.tag.dto.UpdateTagRequest;
import com.bego.backend.tag.entity.TagEntity;
import com.bego.backend.tag.mapper.TagMapper;
import com.bego.backend.tag.vo.TagResponse;
import com.bego.backend.todo.mapper.TodoTagMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {
    private static final String DEFAULT_COLOR = "#2F80ED";
    private static final String COLOR_PATTERN = "^#[0-9A-Fa-f]{6}$";

    private final TagMapper tagMapper;
    private final TodoTagMapper todoTagMapper;

    public TagService(TagMapper tagMapper, TodoTagMapper todoTagMapper) {
        this.tagMapper = tagMapper;
        this.todoTagMapper = todoTagMapper;
    }

    public List<TagResponse> list(Long userId) {
        return tagMapper.findActiveResponsesByUserId(userId);
    }

    @Transactional
    public TagResponse create(Long userId, CreateTagRequest request) {
        TagEntity tag = new TagEntity();
        tag.setUserId(userId);
        tag.setName(cleanName(request.name()));
        tag.setNormalizedName(normalizeName(request.name()));
        tag.setColor(cleanColor(request.color()));
        tag.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        tag.setActiveKey(0L);

        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(ErrorCode.TAG_NAME_ALREADY_EXISTS);
        }

        TagEntity created = tagMapper.findActiveByIdAndUserId(tag.getId(), userId);
        return TagResponse.from(created, 0);
    }

    @Transactional
    public TagResponse update(Long userId, Long tagId, UpdateTagRequest request) {
        TagEntity existing = getRequiredActiveTag(userId, tagId);
        String normalizedName = normalizeName(request.name());
        TagEntity sameName = tagMapper.findActiveByUserIdAndNormalizedName(userId, normalizedName);
        if (sameName != null && !sameName.getId().equals(tagId)) {
            throw new ApiException(ErrorCode.TAG_NAME_ALREADY_EXISTS);
        }

        existing.setName(cleanName(request.name()));
        existing.setNormalizedName(normalizedName);
        existing.setColor(cleanColor(request.color()));
        existing.setSortOrder(request.sortOrder() == null ? existing.getSortOrder() : request.sortOrder());

        try {
            int updated = tagMapper.update(existing);
            if (updated == 0) {
                throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
            }
        } catch (DuplicateKeyException exception) {
            throw new ApiException(ErrorCode.TAG_NAME_ALREADY_EXISTS);
        }

        TagEntity updated = getRequiredActiveTag(userId, tagId);
        long openTodoCount = tagMapper.countOpenTodosByIdAndUserId(tagId, userId);
        return TagResponse.from(updated, openTodoCount);
    }

    @Transactional
    public void delete(Long userId, Long tagId) {
        getRequiredActiveTag(userId, tagId);
        todoTagMapper.deleteByTagIdAndUserId(tagId, userId);
        int deleted = tagMapper.softDeleteByIdAndUserId(tagId, userId);
        if (deleted == 0) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Transactional
    public void reorder(Long userId, ReorderTagsRequest request) {
        List<ReorderTagItem> items = request.items();
        List<Long> ids = items.stream().map(ReorderTagItem::id).toList();
        Set<Long> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tag ids must be unique");
        }

        int existingCount = tagMapper.countActiveByUserIdAndIds(userId, ids);
        if (existingCount != ids.size()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        for (ReorderTagItem item : items) {
            tagMapper.updateSortOrder(item.id(), userId, item.sortOrder());
        }
    }

    private TagEntity getRequiredActiveTag(Long userId, Long tagId) {
        TagEntity tag = tagMapper.findActiveByIdAndUserId(tagId, userId);
        if (tag == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return tag;
    }

    private String cleanName(String name) {
        return name.trim();
    }

    private String normalizeName(String name) {
        return cleanName(name).toLowerCase(Locale.ROOT);
    }

    private String cleanColor(String color) {
        if (color == null || color.isBlank()) {
            return DEFAULT_COLOR;
        }
        String trimmedColor = color.trim();
        if (!trimmedColor.matches(COLOR_PATTERN)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tag color must be a hex color like #2F80ED");
        }
        return trimmedColor.toUpperCase(Locale.ROOT);
    }
}
