package io.github.lischenerks.taskmanagement.service;

import io.github.lischenerks.taskmanagement.Task;
import io.github.lischenerks.taskmanagement.TaskPriority;
import io.github.lischenerks.taskmanagement.TaskStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {
    private final Map<Long, Task> tasks = new HashMap<>();

    public TaskService() {
        tasks.put(1L, new Task(
                1L,
                1L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3),
                TaskPriority.MEDIUM
        ));
        tasks.put(2L, new Task(
                2L,
                1L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                TaskPriority.HIGH
        ));
        tasks.put(3L, new Task(
                3L,
                1L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                TaskPriority.LOW
        ));
    }

    public Task getTaskById(Long id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new IllegalArgumentException("task with id = %s does not exists".formatted(id));
        }
        return task;
    }

    public List<Task> getAllTasks() {
        return tasks.values().stream().toList();
    }
}
