package io.github.lischenerks.taskmanagement.service;

import io.github.lischenerks.taskmanagement.domain.Task;
import io.github.lischenerks.taskmanagement.mapper.TaskMapper;
import io.github.lischenerks.taskmanagement.domain.TaskStatus;
import io.github.lischenerks.taskmanagement.repository.TaskEntity;
import io.github.lischenerks.taskmanagement.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    private final TaskRepository repository;
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskMapper mapper;

    public TaskService(TaskRepository repository, TaskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        log.info("called method getTaskById with id = {}", id);
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException(
                "Not found task with id = " + id));
        return mapper.toDomain(taskEntity);
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasksWithFilters(TaskSearchFilter filter, Pageable pageable) {
        log.info("called method getAllTasksWithFilters");

        return repository.getAllTasksWithFilters(
                filter.creatorId(),
                filter.assignedUserId(),
                filter.status(),
                filter.priority(),
                pageable
        ).stream().map(mapper::toDomain).toList();
    }

    @Transactional
    public Task createTask(Task task) {
        log.info("called method createTask");
        if (task.id() != null) {
            throw new IllegalArgumentException("created tasks id must be null");
        }
        TaskEntity createdTask = mapper.toEntity(task);
        createdTask.setStatus(TaskStatus.CREATED);
        TaskEntity savedTask = repository.save(createdTask);
        log.info("method createTask saved task with id = {}", savedTask.getId());
        return mapper.toDomain(savedTask);
    }

    @Transactional
    public Task updateTask(Long id, Task task) {
        log.info("called method updateTask with id = {}", id);
        if (task.id() != null) {
            throw new IllegalArgumentException("updated tasks id must be null");
        }
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException(
                "Not found task with id = " + id));
        if (taskEntity.getStatus() == TaskStatus.DONE && task.status() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "can not set status %S to task with status DONE. You can set only status IN_PROGRESS".formatted(
                            task.status()));
        }
        taskEntity.setCreatorId(task.creatorId());
        taskEntity.setAssignedUserId(task.assignedUserId());
        taskEntity.setCreateDateTime(task.createDateTime());
        taskEntity.setStatus(task.status());
        taskEntity.setDeadlineDate(task.deadlineDate());
        taskEntity.setPriority(task.priority());
        TaskEntity updatedTask = repository.save(taskEntity);
        log.info("method updateTask updated task with id = {}", updatedTask.getId());
        return mapper.toDomain(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        log.info("called method deleteTask with id = {}", id);
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException(
                "Not found task with id = " + id));
        repository.delete(taskEntity);
        log.info("method deleteTask deleted task with id = {}", id);
    }

    @Transactional
    public Task startTask(Long id) {
        log.info("called method startTask with id = {}", id);
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException(
                "Not found task with id = " + id));
        if (taskEntity.getAssignedUserId() == null) {
            throw new IllegalStateException("assignedUserId must be filled before task starts");
        }
        Long userId = taskEntity.getAssignedUserId();

        repository.lockTasksByAssignedUserId(userId);

        if (taskEntity.getStatus() == TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("can not start task with status IN_PROGRESS");
        }

        Integer numberOfStartedTasksOfThisUser = repository.countByAssignedUserIdAndStatus(userId,
                TaskStatus.IN_PROGRESS);

        if (numberOfStartedTasksOfThisUser >= 5) {
            throw new IllegalStateException("user already has 5 active tasks (IN_PROGRESS). Can not assign more");
        }

        taskEntity.setStatus(TaskStatus.IN_PROGRESS);
        TaskEntity startedTask = repository.save(taskEntity);
        log.info("method startTask started task with id = {}", id);
        return mapper.toDomain(startedTask);
    }

    @Transactional
    public Task completeTask(Long id) {
        log.info("called method completeTask with id = {}", id);
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException(
                "Not found task with id = " + id));
        if (taskEntity.getAssignedUserId() == null) {
            throw new IllegalStateException("can not complete task without filled field assignedUserId");
        }
        if (taskEntity.getDeadlineDate() == null) {
            throw new IllegalStateException("can not complete task without filled field deadlineDate");
        }
        taskEntity.setStatus(TaskStatus.DONE);
        taskEntity.setDoneDateTime(LocalDateTime.now());

        TaskEntity completedTask = repository.save(taskEntity);
        log.info("method completeTask completed task with id = {}", id);
        return mapper.toDomain(completedTask);
    }
}
