package io.github.lischenerks.taskmanagement.service;

import io.github.lischenerks.taskmanagement.model.TaskPriority;
import io.github.lischenerks.taskmanagement.model.TaskStatus;

public record TaskSearchFilter(
        Long creatorId,
        Long assignedUserId,
        TaskStatus status,
        TaskPriority priority,
        Integer pageSize,
        Integer pageNumber) {
}
