package io.github.lischenerks.taskmanagement.dto;

import io.github.lischenerks.taskmanagement.domain.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateTaskDto(
        @NotNull Long assignedUserId,

        @Schema(
                example = "2077-12-31T23:59:59",
                description = "must be in future"
        ) @NotNull @Future LocalDateTime deadlineDate,
        @NotNull TaskPriority priority
) {
}
