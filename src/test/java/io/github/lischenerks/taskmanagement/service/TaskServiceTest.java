package io.github.lischenerks.taskmanagement.service;

import io.github.lischenerks.taskmanagement.domain.Task;
import io.github.lischenerks.taskmanagement.mapper.TaskMapper;
import io.github.lischenerks.taskmanagement.domain.TaskPriority;
import io.github.lischenerks.taskmanagement.domain.TaskStatus;
import io.github.lischenerks.taskmanagement.repository.TaskEntity;
import io.github.lischenerks.taskmanagement.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskRepository repository;

    @Spy
    private TaskMapper mapper;

    @Test
    void createTask_withNotNullId_throwsException() {
        Task task = new Task(
                1L,
                0L,
                0L,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));
        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(task));
    }

    @Test
    void createTask_withValidTask_savesTask() {
        Task task = new Task(
                null,
                0L,
                0L,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));

        TaskEntity taskEntity = mapper.toEntity(task);
        taskEntity.setStatus(TaskStatus.CREATED);
        taskEntity.setId(1L);

        Mockito.when(repository.save(Mockito.any(TaskEntity.class))).thenReturn(taskEntity);

        Task createdTask = mapper.toDomain(taskEntity);
        Assertions.assertEquals(createdTask, taskService.createTask(task));

        Mockito.verify(repository, Mockito.times(1)).save(Mockito.any(TaskEntity.class));

    }

    @Test
    void startTask_withNullAssignedUserId_throwsException() {
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                1L,
                0L,
                null,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));
        assertThrows(IllegalStateException.class, () -> taskService.startTask(id));
    }

    @Test
    void startTask_withInProgressStatus_throwsException() {
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                1L,
                0L,
                1L,
                TaskStatus.IN_PROGRESS,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));
        assertThrows(IllegalStateException.class, () -> taskService.startTask(id));
    }

    @Test
    void startTask_toUserWithFiveInProgressTasks_throwsException() {
        long assignedUserId = 1L;
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                id,
                0L,
                assignedUserId,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));
        Mockito.when(
                repository.countByAssignedUserIdAndStatus(
                        assignedUserId,
                        TaskStatus.IN_PROGRESS)).thenReturn(5);
        assertThrows(IllegalStateException.class, () -> taskService.startTask(id));
    }

    @Test
    void completeTask_withNullAssignedUserId_throwsException() {
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                id,
                0L,
                null,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));
        assertThrows(IllegalStateException.class, () -> taskService.completeTask(id));
    }

    @Test
    void completeTask_withNullDeadlineDate_throwsException() {
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                id,
                0L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                null,
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));
        assertThrows(IllegalStateException.class, () -> taskService.completeTask(id));
    }

    @Test
    void completeTask_withValidTask_savesTaskWithNowDoneDateTime() {
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                id,
                0L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                null);

        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));
        Mockito.when(repository.save(taskEntity)).thenReturn(taskEntity);

        LocalDateTime startTime = LocalDateTime.now();
        Task completedTask = taskService.completeTask(id);
        LocalDateTime endTime = LocalDateTime.now();

        assertTrue(startTime.isBefore(completedTask.doneDateTime()));
        assertTrue(endTime.isAfter(completedTask.doneDateTime()));
    }

    @Test
    void completeTask_withValidTask_savesTaskWithDoneStatus() {
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                id,
                0L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                null);

        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));
        Mockito.when(repository.save(taskEntity)).thenReturn(taskEntity);

        Task completedTask = taskService.completeTask(id);

        Assertions.assertEquals(TaskStatus.DONE, completedTask.status());
    }

    @Test
    void completeTask_withValidTask_savesTask() {
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                id,
                0L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                null);

        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));
        Mockito.when(repository.save(taskEntity)).thenReturn(taskEntity);

        taskService.completeTask(id);
        Mockito.verify(repository, Mockito.times(1)).save(taskEntity);
    }

    @Test
    void getTaskById_withNotExistsTaskId_throwsException() {
        long id = 1L;
        Mockito.when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.getTaskById(id));
    }

    @Test
    void deleteTaskById_withNotExistsTaskId_throwsException() {
        long id = 1L;
        Mockito.when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.deleteTask(id));
    }

    @Test
    void getAllTasksWithFilters_getTasks() {
        TaskSearchFilter filter = new TaskSearchFilter(
                1L,
                1L,
                TaskStatus.CREATED,
                TaskPriority.MEDIUM
        );

        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));

        List<TaskEntity> repositoryResponse = new ArrayList<>();
        repositoryResponse.add(new TaskEntity(
                0L,
                0L,
                0L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.MEDIUM,
                null)
        );
        repositoryResponse.add(new TaskEntity(
                1L,
                1L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.MEDIUM,
                null)
        );
        repositoryResponse.add(new TaskEntity(
                2L,
                2L,
                2L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.MEDIUM,
                null)
        );

        Mockito.when(repository.getAllTasksWithFilters(
                filter.creatorId(),
                filter.assignedUserId(),
                filter.status(),
                filter.priority(),
                pageable
        )).thenReturn(repositoryResponse);

        assertEquals(taskService.getAllTasksWithFilters(filter, pageable),
                repositoryResponse.stream().map(mapper::toDomain).toList());

        Mockito.verify(repository, Mockito.times(1)).getAllTasksWithFilters(
                filter.creatorId(),
                filter.assignedUserId(),
                filter.status(),
                filter.priority(),
                pageable
        );
    }
}
