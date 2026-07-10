package io.github.lischenerks.taskmanagement.repository;


import io.github.lischenerks.taskmanagement.TaskPriority;
import io.github.lischenerks.taskmanagement.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    Integer countByAssignedUserIdAndStatus(Long assignedUserId, TaskStatus status);

    @Query("""
                SELECT t from TaskEntity t
                WHERE (:creatorId is null OR t.creatorId = :creatorId) AND
                (:assignedUserId is null OR t.assignedUserId = :assignedUserId) AND
                (:status is null OR t.status = :status) AND
                (:priority is null OR t.priority = :priority)
            """)
    List<TaskEntity> getAllTasksWithFilters(
            @Param("creatorId") Long creatorId,
            @Param("assignedUserId") Long assignedUserId,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            Pageable pageable
    );
}
