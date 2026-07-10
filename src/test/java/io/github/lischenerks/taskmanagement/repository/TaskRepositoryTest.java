package io.github.lischenerks.taskmanagement.repository;

import io.github.lischenerks.taskmanagement.TaskPriority;
import io.github.lischenerks.taskmanagement.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class TaskRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TaskRepository repository;

    private static TaskEntity entity1;

    private static TaskEntity entity2;

    private static TaskEntity entity3;

    @BeforeAll
    static void setUp(@Autowired TaskRepository repository) {
        entity1 = repository.save(
                new TaskEntity(
                        null,
                        0L,
                        0L,
                        TaskStatus.CREATED,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(1),
                        TaskPriority.LOW,
                        LocalDateTime.now().plusDays(2)));

        entity2 = repository.save(
                new TaskEntity(
                        null,
                        1L,
                        1L,
                        TaskStatus.IN_PROGRESS,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(1),
                        TaskPriority.MEDIUM,
                        LocalDateTime.now().plusDays(2)));

        entity3 = repository.save(
                new TaskEntity(
                        null,
                        2L,
                        2L,
                        TaskStatus.DONE,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(1),
                        TaskPriority.HIGH,
                        LocalDateTime.now().plusDays(2)));
    }

    @Test
    void getAllTasksWithFilters_filtersByStatus() {
        Assertions.assertEquals(
                repository.getAllTasksWithFilters(
                        null,
                        null,
                        TaskStatus.CREATED,
                        null,
                        Pageable.ofSize(10)),
                List.of(entity1));

        Assertions.assertEquals(
                repository.getAllTasksWithFilters(
                        null,
                        null,
                        TaskStatus.IN_PROGRESS,
                        null,
                        Pageable.ofSize(10)),
                List.of(entity2));

        Assertions.assertEquals(
                repository.getAllTasksWithFilters(
                        null,
                        null,
                        TaskStatus.DONE,
                        null,
                        Pageable.ofSize(10)),
                List.of(entity3));
    }

    @Test
    void getAllTasksWithFilters_filtersByPriority() {
        Assertions.assertEquals(
                repository.getAllTasksWithFilters(
                        null,
                        null,
                        null,
                        TaskPriority.LOW,
                        Pageable.ofSize(10)),
                List.of(entity1));

        Assertions.assertEquals(
                repository.getAllTasksWithFilters(
                        null,
                        null,
                        null,
                        TaskPriority.MEDIUM,
                        Pageable.ofSize(10)),
                List.of(entity2));

        Assertions.assertEquals(
                repository.getAllTasksWithFilters(
                        null,
                        null,
                        null,
                        TaskPriority.HIGH,
                        Pageable.ofSize(10)),
                List.of(entity3));
    }

    @Test
    void getAllTasksWithFilters_filtersByAssignedUserId() {
        Assertions.assertEquals(
                repository.getAllTasksWithFilters(null, 0L, null, null, Pageable.ofSize(10)),
                List.of(entity1));

        Assertions.assertEquals(
                repository.getAllTasksWithFilters(null, 1L, null, null, Pageable.ofSize(10)),
                List.of(entity2));

        Assertions.assertEquals(
                repository.getAllTasksWithFilters(null, 2L, null, null, Pageable.ofSize(10)),
                List.of(entity3));
    }

    @Test
    void getAllTasksWithFilters_filtersByCreatorId() {
        Assertions.assertEquals(
                repository.getAllTasksWithFilters(0L, null, null, null, Pageable.ofSize(10)),
                List.of(entity1));

        Assertions.assertEquals(
                repository.getAllTasksWithFilters(1L, null, null, null, Pageable.ofSize(10)),
                List.of(entity2));

        Assertions.assertEquals(
                repository.getAllTasksWithFilters(2L, null, null, null, Pageable.ofSize(10)),
                List.of(entity3));
    }

    @Test
    void getAllTasksWithFilters_withoutFilters() {
        Assertions.assertEquals(
                repository.getAllTasksWithFilters(null, null, null, null, Pageable.ofSize(10)),
                List.of(entity1, entity2, entity3));
    }
}
