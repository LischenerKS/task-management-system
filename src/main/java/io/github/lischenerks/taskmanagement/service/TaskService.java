package io.github.lischenerks.taskmanagement.service;

import io.github.lischenerks.taskmanagement.Task;
import io.github.lischenerks.taskmanagement.TaskMapper;
import io.github.lischenerks.taskmanagement.TaskStatus;
import io.github.lischenerks.taskmanagement.repository.TaskEntity;
import io.github.lischenerks.taskmanagement.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public Task getTaskById(Long id) {
        log.info("called method getTaskById with id = {}", id);
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found task with id = " + id));
        return mapper.toTask(taskEntity);
    }

    public List<Task> getAllTasks() {
        log.info("called method getAllTasks");
        return repository.findAll().stream().map(mapper::toTask).toList();
    }

    public Task createTask(Task task) {
        log.info("called method createTask");
        if (task.status() != null) {
            throw new IllegalArgumentException("created tasks status must be null");
        }
        if (task.id() != null) {
            throw new IllegalArgumentException("created tasks id must be null");
        }

        TaskEntity createdTask = mapper.toTaskEntity(task);
        createdTask.setStatus(TaskStatus.CREATED);
        TaskEntity savedTask =  repository.save(createdTask);
        log.info("method createTask saved task with id = {}", savedTask.getId());
        return mapper.toTask(savedTask);
    }

    public Task updateTask(Long id, Task task) {
        log.info("called method updateTask with id = {}", id);
        if (task.id() != null) {
            throw new IllegalArgumentException("created tasks id must be null");
        }
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException(
                "Not found task with id = " + id));
        if (taskEntity.getStatus() == TaskStatus.DONE) {
            throw new IllegalStateException("can not update task with status DONE");
        }
        taskEntity.setCreatorId(task.creatorId());
        taskEntity.setAssignedUserId(task.assignedUserId());
        taskEntity.setCreateDateTime(task.createDateTime());
        taskEntity.setStatus(task.status());
        taskEntity.setDeadlineDate(task.deadlineDate());
        taskEntity.setPriority(task.priority());
        TaskEntity updatedTask = repository.save(taskEntity);
        log.info("method updateTask updated task with id = {}", updatedTask.getId());
        return mapper.toTask(updatedTask);
    }

    public void deleteTask(Long id) {
        log.info("called method deleteTask with id = {}", id);
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found task with id = " + id));
        repository.delete(taskEntity);
        log.info("method deleteTask deleted task with id = {}", id);
    }

    public void startTask(Long id) {
        log.info("called method startTask with id = {}", id);
        TaskEntity taskEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found task with id = " + id));
        if (taskEntity.getAssignedUserId() == null) {
            throw new IllegalStateException("assignedUserId must be filled before task starts");
        }
        Long userId = taskEntity.getAssignedUserId();
        Integer numberOfStartedTasksOfThisUser = repository.countByAssignedUserIdAndStatus(userId, TaskStatus.IN_PROGRESS);

        if (numberOfStartedTasksOfThisUser >= 5) {
            throw new IllegalStateException("can not start task to user who has more than 5 tasks");
        }

        taskEntity.setStatus(TaskStatus.IN_PROGRESS);
        repository.save(taskEntity);
        log.info("method startTask started task with id = {}", id);
    }
}
