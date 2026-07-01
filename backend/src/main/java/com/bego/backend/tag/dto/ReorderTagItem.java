package com.bego.backend.tag.dto;

import jakarta.validation.constraints.NotNull;

public record ReorderTagItem(
        @NotNull Long id,
        @NotNull Integer sortOrder
) {
}
