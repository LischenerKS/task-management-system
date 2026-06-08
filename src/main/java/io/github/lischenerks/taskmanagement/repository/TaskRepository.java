package io.github.lischenerks.taskmanagement.repository;


import io.github.lischenerks.taskmanagement.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    Integer countByAssignedUserIdAndStatus(Long assignedUserId, TaskStatus status);
}
