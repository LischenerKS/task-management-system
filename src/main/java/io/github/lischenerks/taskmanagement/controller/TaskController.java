package io.github.lischenerks.taskmanagement.controller;

import io.github.lischenerks.taskmanagement.Task;
import io.github.lischenerks.taskmanagement.model.TaskPriority;
import io.github.lischenerks.taskmanagement.model.TaskStatus;
import io.github.lischenerks.taskmanagement.service.TaskSearchFilter;
import io.github.lischenerks.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/tasks")
@RestController
public class TaskController {
    private final TaskService taskService;

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasksWithFilters(
            @RequestParam(name = "creatorId", required = false) Long creatorId,
            @RequestParam(name = "assignedUserId", required = false) Long assignedUserId,
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "priority", required = false) TaskPriority priority,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "pageNumber", required = false) Integer pageNumber
    ) {
        log.info("called method getAllTasksWithFilters");
        TaskSearchFilter fIlter = new TaskSearchFilter(
                creatorId,
                assignedUserId,
                status,
                priority,
                pageSize,
                pageNumber
        );
        var tasks = taskService.getAllTasksWithFilters(fIlter);
        log.info("method getAllTasksWithFilters return {} tasks", tasks.size());
        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
            @PathVariable("id") Long id
    ) {
        log.info("called method getTestById with id= {}", id);
        var task = taskService.getTaskById(id);
        log.info("method getTestById return task with id= {}", task.id());
        return ResponseEntity.status(HttpStatus.OK).body(task);

    }

    @PostMapping()
    public ResponseEntity<Task> createTask(
            @Valid @RequestBody Task task
    ) {
        log.info("called method createTask");
        var createdTask = taskService.createTask(task);
        log.info("method createTask successfully ended");
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable("id") Long id,
            @Valid @RequestBody Task task
    ) {
        log.info("called method updateTask with id = {}", id);
        var updatedTask = taskService.updateTask(id, task);
        log.info("method updateTask successfully ended");
        return ResponseEntity.status(HttpStatus.OK).body(updatedTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("id") Long id
    ) {
        log.info("called method deleteTask with id = {}", id);
        taskService.deleteTask(id);
        log.info("method deleteTask with id = {} successfully ended", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    @PostMapping("/{id}/start")
    public ResponseEntity<Task> startTask(
            @PathVariable("id") Long id
    ) throws InterruptedException {
        log.info("called method startTask with id = {}", id);
        var startedTask = taskService.startTask(id);
        log.info("method startTask with id = {} successfully ended", id);
        return ResponseEntity.status(HttpStatus.OK).body(startedTask);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Task> completeTask(
            @PathVariable("id") Long id
    ) {
        log.info("called method completeTask with id = {}", id);
        var completedTask = taskService.completeTask(id);
        log.info("method completeTask with id = {} successfully ended", id);
        return ResponseEntity.status(HttpStatus.OK).body(completedTask);
    }
}
