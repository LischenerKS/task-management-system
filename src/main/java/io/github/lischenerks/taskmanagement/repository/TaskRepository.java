package io.github.lischenerks.taskmanagement.repository;


import io.github.lischenerks.taskmanagement.domain.TaskPriority;
import io.github.lischenerks.taskmanagement.domain.TaskStatus;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
                        SELECT t from TaskEntity t
                        WHERE t.assignedUserId = :assignedUserId
                """)
    List<TaskEntity> lockTasksByAssignedUserId(
            @Param("assignedUserId") Long assignedUserId
    );
}
