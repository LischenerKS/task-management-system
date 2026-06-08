package io.github.lischenerks.taskmanagement.controller;

import io.github.lischenerks.taskmanagement.Task;
import io.github.lischenerks.taskmanagement.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public List<Task> getAllTasks() {
        log.info("call method getAllTasks");
        var tasks = taskService.getAllTasks();
        log.info("method getAllTasks return {} tasks", tasks.size());
        return tasks;
    }


    @GetMapping("/{id}")
    public Task getTastById(
            @PathVariable("id") Long id
    ) {
        log.info("call method getTestById with id={}", id);
        var task = taskService.getTaskById(id);
        log.info("method getTestById return task with id={}", task.id());
        return task;
    }

}
