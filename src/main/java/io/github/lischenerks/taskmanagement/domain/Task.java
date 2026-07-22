package io.github.lischenerks.taskmanagement.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Task(
        Long id,

        Long creatorId,
        Long assignedUserId,
        TaskStatus status,
        LocalDateTime createDateTime,

        LocalDateTime deadlineDate,

        TaskPriority priority,

        LocalDateTime doneDateTime
) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
