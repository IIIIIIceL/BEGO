package com.bego.backend.tag.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderTagsRequest(
        @NotEmpty List<@Valid ReorderTagItem> items
) {
}
