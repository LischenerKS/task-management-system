package io.github.lischenerks.taskmanagement.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.lischenerks.taskmanagement.domain.Task;
import io.github.lischenerks.taskmanagement.domain.TaskPriority;
import io.github.lischenerks.taskmanagement.domain.TaskStatus;
import io.github.lischenerks.taskmanagement.dto.CreateTaskDto;
import io.github.lischenerks.taskmanagement.dto.TaskResponseDto;
import io.github.lischenerks.taskmanagement.mapper.TaskMapper;
import io.github.lischenerks.taskmanagement.repository.TaskEntity;
import io.github.lischenerks.taskmanagement.service.TaskSearchFilter;
import io.github.lischenerks.taskmanagement.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TaskMapper.class)
@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @MockitoBean
    private TaskService taskService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskMapper mapper;

    private ObjectMapper objectMapper;

    private Task task;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        task = new Task(
                null,
                0L,
                1L,
                TaskStatus.CREATED,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2)
        );

    }

    @Test
    void getAllTasksWithFilters() throws Exception {
        TaskSearchFilter filter = new TaskSearchFilter(
                null,
                null,
                null,
                null
        );


        Mockito.when(taskService.getAllTasksWithFilters(Mockito.eq(filter), Mockito.any(Pageable.class))).thenReturn(
                List.of(task));

        List<TaskResponseDto> responseList = List.of(task).stream().map(mapper::toResponse).toList();


        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<TaskResponseDto> responsePage = new PageImpl<>(responseList, pageable, responseList.size());

        String expectedJson = objectMapper.writeValueAsString(responsePage);

        mockMvc.perform(get("/tasks")).andExpect(status().isOk()).andExpect(content().json(expectedJson));

        verify(taskService, times(1)).getAllTasksWithFilters(filter, pageable);
    }

    @Test
    void getTaskById() throws Exception {
        String expectedJson = objectMapper.writeValueAsString(task);

        Mockito.when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get("/tasks/{id}", 1L)).andExpect(status().isOk()).andExpect(content().json(expectedJson));

        verify(taskService, times(1)).getTaskById(1L);
    }

    @Test
    void createTask() throws Exception {
        CreateTaskDto createTaskDto = new CreateTaskDto(
                task.creatorId(),
                task.assignedUserId(),
                task.deadlineDate(),
                task.priority()
        );

        String createTaskDtoJson = objectMapper.writeValueAsString(createTaskDto);
        String taskJson = objectMapper.writeValueAsString(task);

        Mockito.when(taskService.createTask(task)).thenReturn(task);

        mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(createTaskDtoJson)
        ).andExpect(status().isCreated()).andExpect(content().json(taskJson));

        verify(taskService, times(1)).createTask(task);
    }

    @Test
    void updateTask() throws Exception {
        String taskJson = objectMapper.writeValueAsString(task);
        Mockito.when(taskService.updateTask(1L, task)).thenReturn(task);

        mockMvc.perform(put("/tasks/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(taskJson)
        ).andExpect(status().isOk()).andExpect(content().json(taskJson));

        verify(taskService, times(1)).updateTask(1L, task);
    }

    @Test
    void updateTask_returnsConflict_whenOptimisticLockFails() throws Exception {
        String taskJson = objectMapper.writeValueAsString(task);
        Mockito.when(taskService.updateTask(1L, task)).thenThrow(new ObjectOptimisticLockingFailureException(
                TaskEntity.class,
                1L));

        mockMvc.perform(put("/tasks/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(taskJson)
        ).andExpect(status().isConflict());

        verify(taskService, times(1)).updateTask(1L, task);
    }

    @Test
    void startTask() throws Exception {
        Task startedTask = new Task(
                task.id(),
                task.creatorId(),
                task.assignedUserId(),
                TaskStatus.IN_PROGRESS,
                task.createDateTime(),
                task.deadlineDate(),
                task.priority(),
                task.doneDateTime()
        );

        String startedTaskJson = objectMapper.writeValueAsString(startedTask);
        Mockito.when(taskService.startTask(1L)).thenReturn(startedTask);
        mockMvc.perform(post("/tasks/{id}/start", 1L).contentType(MediaType.APPLICATION_JSON).content(
                startedTaskJson)).andExpect(status().isOk());

        verify(taskService, times(1)).startTask(1L);
    }

    @Test
    void completeTask() throws Exception {
        Task completedTask = new Task(
                task.id(),
                task.creatorId(),
                task.assignedUserId(),
                TaskStatus.DONE,
                task.createDateTime(),
                task.deadlineDate(),
                task.priority(),
                task.doneDateTime()
        );
        String completedTaskJson = objectMapper.writeValueAsString(completedTask);
        Mockito.when(taskService.completeTask(1L)).thenReturn(completedTask);

        mockMvc.perform(post("/tasks/{id}/complete", 1L).contentType(MediaType.APPLICATION_JSON).content(
                completedTaskJson)).andExpect(status().isOk());
        verify(taskService, times(1)).completeTask(1L);
    }
}
