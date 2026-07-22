package io.github.lischenerks.taskmanagement.repository;

import io.github.lischenerks.taskmanagement.domain.TaskPriority;
import io.github.lischenerks.taskmanagement.domain.TaskStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "task")
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TaskStatus status;

    @Column(name = "create_date_time")
    private LocalDateTime createDateTime;

    @Column(name = "deadline_date")
    private LocalDateTime deadlineDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private TaskPriority priority;

    @Version
    @Column(name = "version")
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskEntity that = (TaskEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public TaskEntity(
            Long id,
            Long creatorId,
            Long assignedUserId,
            TaskStatus status,
            LocalDateTime createDateTime,
            LocalDateTime deadlineDate,
            TaskPriority priority,
            LocalDateTime doneDateTime) {
        this.id = id;
        this.creatorId = creatorId;
        this.assignedUserId = assignedUserId;
        this.status = status;
        this.createDateTime = createDateTime;
        this.deadlineDate = deadlineDate;
        this.priority = priority;
        this.doneDateTime = doneDateTime;
    }

    @Column(name = "done_date_time")
    private LocalDateTime doneDateTime;

    public TaskEntity() {
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(LocalDateTime createDateTime) {
        this.createDateTime = createDateTime;
    }

    public LocalDateTime getDeadlineDate() {
        return deadlineDate;
    }

    public void setDeadlineDate(LocalDateTime deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public LocalDateTime getDoneDateTime() {
        return doneDateTime;
    }

    public void setDoneDateTime(LocalDateTime doneDateTime) {
        this.doneDateTime = doneDateTime;
    }
}
