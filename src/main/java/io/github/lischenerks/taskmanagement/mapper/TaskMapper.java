package io.github.lischenerks.taskmanagement.mapper;

import io.github.lischenerks.taskmanagement.dto.CreateTaskDto;
import io.github.lischenerks.taskmanagement.dto.TaskResponseDto;
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

    public Task toDomain(CreateTaskDto createTaskDto) {
        return new Task(
                null,
                createTaskDto.creatorId(),
                createTaskDto.assignedUserId(),
                null,
                null,
                createTaskDto.deadlineDate(),
                createTaskDto.priority(),
                null
        );
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

    public TaskResponseDto toResponse(Task task) {
        return new TaskResponseDto(
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
