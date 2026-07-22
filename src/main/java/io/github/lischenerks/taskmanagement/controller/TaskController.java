package io.github.lischenerks.taskmanagement.controller;

import io.github.lischenerks.taskmanagement.dto.CreateTaskDto;
import io.github.lischenerks.taskmanagement.dto.TaskResponseDto;
import io.github.lischenerks.taskmanagement.domain.Task;
import io.github.lischenerks.taskmanagement.domain.TaskPriority;
import io.github.lischenerks.taskmanagement.domain.TaskStatus;
import io.github.lischenerks.taskmanagement.dto.UpdateTaskDto;
import io.github.lischenerks.taskmanagement.mapper.TaskMapper;
import io.github.lischenerks.taskmanagement.service.TaskSearchFilter;
import io.github.lischenerks.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/tasks")
@RestController
public class TaskController {
    private final TaskService taskService;

    private final TaskMapper mapper;

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    public TaskController(TaskService taskService, TaskMapper mapper) {
        this.taskService = taskService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponseDto>> getAllTasksWithFilters(
            @RequestParam(name = "creatorId", required = false) Long creatorId,
            @RequestParam(name = "assignedUserId", required = false) Long assignedUserId,
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "priority", required = false) TaskPriority priority,
            @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable
    ) {
        log.info("called method getAllTasksWithFilters");
        TaskSearchFilter filter = new TaskSearchFilter(
                creatorId,
                assignedUserId,
                status,
                priority
        );
        List<Task> tasks = taskService.getAllTasksWithFilters(filter, pageable);
        log.info("method getAllTasksWithFilters return {} tasks", tasks.size());

        List<TaskResponseDto> responseList = tasks.stream().map(mapper::toResponse).toList();

        Page<TaskResponseDto> responsePage = new PageImpl<>(responseList, pageable, responseList.size());
        return ResponseEntity.status(HttpStatus.OK).body(responsePage);
    }


    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTaskById(
            @PathVariable("id") Long id
    ) {
        log.info("called method getTestById with id= {}", id);
        var task = taskService.getTaskById(id);
        log.info("method getTestById return task with id= {}", task.id());
        return ResponseEntity.status(HttpStatus.OK).body(mapper.toResponse(task));

    }

    @PostMapping()
    public ResponseEntity<TaskResponseDto> createTask(
            @Valid @RequestBody CreateTaskDto dto
    ) {
        log.info("called method createTask");
        var createdTask = taskService.createTask(mapper.toDomain(dto));
        log.info("method createTask successfully ended");
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(createdTask));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTask(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTaskDto dto
    ) {
        log.info("called method updateTask with id = {}", id);
        var updatedTask = taskService.updateTask(id, mapper.toDomain(dto));
        log.info("method updateTask successfully ended");
        return ResponseEntity.status(HttpStatus.OK).body(mapper.toResponse(updatedTask));
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
    ) {
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
