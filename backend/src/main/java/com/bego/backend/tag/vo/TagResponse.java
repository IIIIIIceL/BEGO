package com.bego.backend.tag.vo;

import com.bego.backend.tag.entity.TagEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
