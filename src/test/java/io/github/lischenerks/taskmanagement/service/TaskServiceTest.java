package io.github.lischenerks.taskmanagement.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lischenerks.taskmanagement.Task;
import io.github.lischenerks.taskmanagement.mapper.TaskMapper;
import io.github.lischenerks.taskmanagement.model.TaskPriority;
import io.github.lischenerks.taskmanagement.model.TaskStatus;
import io.github.lischenerks.taskmanagement.repository.TaskEntity;
import io.github.lischenerks.taskmanagement.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskRepository repository;

    @Spy
    private TaskMapper mapper;

    @Test
    void createTask_withNotNullStatus_throwsException() {
        Task task = new Task(
                null,
                0L,
                0L,
                TaskStatus.IN_PROGRESS,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));
        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(task));
    }

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
    void updateTask_setNotInProgressStatusToDoneTask_throwsException() {
        long id = 1L;
        TaskEntity taskEntity = new TaskEntity(
                id,
                0L,
                1L,
                TaskStatus.DONE,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2));

        Mockito.when(repository.findById(id)).thenReturn(Optional.of(taskEntity));

        for (TaskStatus status : TaskStatus.values()) {
            if (status == TaskStatus.IN_PROGRESS) continue;

            Task task = new Task(
                    null,
                    0L,
                    1L,
                    status,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    TaskPriority.HIGH,
                    LocalDateTime.now().plusDays(2));

            assertThrows(IllegalStateException.class, () -> taskService.updateTask(id, task));
        }
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
}
