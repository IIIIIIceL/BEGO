package com.bego.backend.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateTodoStatusRequest(
        @NotBlank @Pattern(regexp = "TODO|DONE") String status
) {
}
