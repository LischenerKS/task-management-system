package io.github.lischenerks.taskmanagement.dto;

import io.github.lischenerks.taskmanagement.domain.TaskPriority;
import io.github.lischenerks.taskmanagement.domain.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponseDTO(
        Long id,
        Long creatorId,
        Long assignedUserId,
        TaskStatus status,
        LocalDateTime createDateTime,
        LocalDateTime deadlineDate,
        TaskPriority priority,
        LocalDateTime doneDateTime
) {
}
