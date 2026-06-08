package io.github.lischenerks.taskmanagement.controller;

import io.github.lischenerks.taskmanagement.Task;
import io.github.lischenerks.taskmanagement.service.TaskService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RequestMapping("/tasks")
@RestController
public class TaskController {
    private final TaskService taskService;

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        log.info("called method getAllTasks");
        var tasks = taskService.getAllTasks();
        log.info("method getAllTasks return {} tasks", tasks.size());
        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Task> getTastById(
            @PathVariable("id") Long id
    ) {
        log.info("called method getTestById with id= {}", id);
        try {
            var task = taskService.getTaskById(id);
            log.info("method getTestById return task with id= {}", task.id());
            return ResponseEntity.status(HttpStatus.OK).body(task);
        } catch (EntityNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping()
    public ResponseEntity<Task> createTask(
            @RequestBody Task task
    ) {
        log.info("called method createTask");
        try {
            var createdTask = taskService.createTask(task);
            log.info("method createTask successfully ended");
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable("id") Long id,
            @RequestBody Task task
        ) {
        log.info("called method updateTask with id = {}", id);
        try {
            var updatedTask = taskService.updateTask(id, task);
            log.info("method updateTask successfully ended");
            return ResponseEntity.status(HttpStatus.OK).body(updatedTask);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (EntityNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("id") Long id
    ) {
        log.info("called method deleteTask with id = {}", id);
        try {
            taskService.deleteTask(id);
            log.info("method deleteTask with id = {} successfully ended", id);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (EntityNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    @GetMapping("/{id}/start")
    public ResponseEntity<String> startTask(
            @PathVariable("id") Long id
    ) {
        log.info("called method startTask with id = {}", id);
        try {
            taskService.startTask(id);
            log.info("method startTask with id = {} successfully ended", id);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (EntityNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
