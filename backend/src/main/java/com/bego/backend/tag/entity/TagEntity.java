package com.bego.backend.tag.entity;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagEntity {
    private Long id;
    private Long userId;
    private String name;
    private String normalizedName;
    private String color;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private Long activeKey;
}
