package io.github.lischenerks.taskmanagement.mapper;

import io.github.lischenerks.taskmanagement.dto.TaskResponseDTO;
import io.github.lischenerks.taskmanagement.domain.Task;
import io.github.lischenerks.taskmanagement.repository.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {
    public Task toDomain(TaskEntity taskEntity) {
        return new Task(
                taskEntity.getId(),
                taskEntity.getCreatorId(),
                taskEntity.getAssignedUserId(),
                taskEntity.getStatus(),
                taskEntity.getCreateDateTime(),
                taskEntity.getDeadlineDate(),
                taskEntity.getPriority(),
                taskEntity.getDoneDateTime());
    }

    public TaskEntity toEntity(Task task) {
        return new TaskEntity(
                task.id(),
                task.creatorId(),
                task.assignedUserId(),
                task.status(),
                task.createDateTime(),
                task.deadlineDate(),
                task.priority(),
                task.doneDateTime());
    }

    public TaskResponseDTO toResponse(Task task) {
        return new TaskResponseDTO(
                task.id(),
                task.creatorId(),
                task.assignedUserId(),
                task.status(),
                task.createDateTime(),
                task.deadlineDate(),
                task.priority(),
                task.doneDateTime()
        );
    }
}
