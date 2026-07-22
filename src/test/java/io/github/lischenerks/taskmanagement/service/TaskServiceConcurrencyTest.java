package io.github.lischenerks.taskmanagement.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lischenerks.taskmanagement.Task;
import io.github.lischenerks.taskmanagement.model.TaskPriority;
import io.github.lischenerks.taskmanagement.model.TaskStatus;
import io.github.lischenerks.taskmanagement.repository.TaskEntity;
import io.github.lischenerks.taskmanagement.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;


@SpringBootTest
@Testcontainers
class TaskServiceConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    private static final long ASSIGNED_USER_ID = 777L;
    private static final int TASK_COUNT = 10;

    @Test
    void startTask_calledConcurrentlyForSameUser_neverExceedsFiveInProgressTasks() throws InterruptedException {
        List<Long> taskIds = new ArrayList<>();
        for (int i = 0; i < TASK_COUNT; i++) {
            TaskEntity saved = taskRepository.save(new TaskEntity(
                    null,
                    1L,
                    ASSIGNED_USER_ID,
                    TaskStatus.CREATED,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    TaskPriority.MEDIUM,
                    null));
            taskIds.add(saved.getId());
        }


        ExecutorService executor = Executors.newFixedThreadPool(TASK_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(TASK_COUNT);
        CountDownLatch done = new CountDownLatch(TASK_COUNT);

        for (Long id : taskIds) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    taskService.startTask(id);
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        boolean finishedInTime = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(finishedInTime, "потоки не завершились за отведённое время");


        int inProgressCount = taskRepository.countByAssignedUserIdAndStatus(ASSIGNED_USER_ID, TaskStatus.IN_PROGRESS);

        assertTrue(
                inProgressCount <= 5,
                "race condition в startTask: у пользователя " + inProgressCount + " задач в IN_PROGRESS, хотя разрешено не более 5");
    }

    @Test
    void updateTask_calledConcurrentlyOnSameTask_rejectsOneOfTheConflictingWrites() throws InterruptedException {
        TaskEntity entity = taskRepository.save(new TaskEntity(
                null,
                1L,
                2L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.LOW,
                null));
        Long id = entity.getId();


        Task updateWithMediumPriority = new Task(
                null,
                entity.getCreatorId(),
                entity.getAssignedUserId(),
                entity.getStatus(),
                entity.getCreateDateTime(),
                entity.getDeadlineDate(),
                TaskPriority.MEDIUM,
                entity.getDoneDateTime());

        Task updateWithHighPriority = new Task(
                null,
                entity.getCreatorId(),
                entity.getAssignedUserId(),
                entity.getStatus(),
                entity.getCreateDateTime(),
                entity.getDeadlineDate(),
                TaskPriority.HIGH,
                entity.getDoneDateTime());

        CyclicBarrier barrier = new CyclicBarrier(2);

        List<Exception> exceptions = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                barrier.await();
                taskService.updateTask(id, updateWithMediumPriority);
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                done.countDown();
            }
        });

        executor.submit(() -> {
            try {
                barrier.await();
                taskService.updateTask(id, updateWithHighPriority);
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                done.countDown();
            }
        });

        boolean finishedInTime = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(finishedInTime, "потоки не завершились за отведённое время");

        // если нет исключения, то произошло конкурентное обновление
        boolean conflictWasDetected = exceptions.stream().anyMatch(
                e -> e instanceof ObjectOptimisticLockingFailureException);

        assertTrue(
                conflictWasDetected,
                "два конкурентных updateTask над одной задачей должны конфликтовать," + "один из них должен быть отклонён с ObjectOptimisticLockingFailureException, " + "но оба тихо прошли (lost update в update task)");
    }

}
