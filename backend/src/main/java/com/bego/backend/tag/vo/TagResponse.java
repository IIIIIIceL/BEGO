package com.bego.backend.tag.vo;

import com.bego.backend.tag.entity.TagEntity;
import java.time.Instant;

public class TagResponse {
    private Long id;
    private String name;
    private String color;
    private Integer sortOrder;
    private Long openTodoCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static TagResponse from(TagEntity tag, long openTodoCount) {
        TagResponse response = new TagResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setColor(tag.getColor());
        response.setSortOrder(tag.getSortOrder());
        response.setOpenTodoCount(openTodoCount);
        response.setCreatedAt(tag.getCreatedAt());
        response.setUpdatedAt(tag.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getOpenTodoCount() {
        return openTodoCount;
    }

    public void setOpenTodoCount(Long openTodoCount) {
        this.openTodoCount = openTodoCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
