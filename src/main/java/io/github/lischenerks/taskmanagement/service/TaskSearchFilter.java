package io.github.lischenerks.taskmanagement.service;

import io.github.lischenerks.taskmanagement.domain.TaskPriority;
import io.github.lischenerks.taskmanagement.domain.TaskStatus;

public record TaskSearchFilter(
        Long creatorId,
        Long assignedUserId,
        TaskStatus status,
        TaskPriority priority) {
}
