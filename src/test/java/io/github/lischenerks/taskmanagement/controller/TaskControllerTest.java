package io.github.lischenerks.taskmanagement.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.lischenerks.taskmanagement.Task;
import io.github.lischenerks.taskmanagement.TaskPriority;
import io.github.lischenerks.taskmanagement.TaskStatus;
import io.github.lischenerks.taskmanagement.service.TaskSearchFilter;
import io.github.lischenerks.taskmanagement.service.TaskService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class TaskControllerTest {

    @Mock private TaskService taskService;

    @InjectMocks private TaskController controller;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private Task task;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        task =
                new Task(
                        null,
                        0L,
                        1L,
                        TaskStatus.CREATED,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(1),
                        TaskPriority.HIGH,
                        LocalDateTime.now().plusDays(2));
    }

    @Test
    void getAllTasksWithFilters() throws Exception {
        TaskSearchFilter filter = new TaskSearchFilter(null, null, null, null, null, null);

        Mockito.when(taskService.getAllTasksWithFilters(filter)).thenReturn(List.of(task));

        String expectedJson = objectMapper.writeValueAsString(List.of(task));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));

        verify(taskService, times(1)).getAllTasksWithFilters(filter);
    }

    @Test
    void getTaskById() throws Exception {
        String expectedJson = objectMapper.writeValueAsString(task);

        Mockito.when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get("/tasks/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));

        verify(taskService, times(1)).getTaskById(1L);
    }

    @Test
    void createTask() throws Exception {
        String taskJson = objectMapper.writeValueAsString(task);
        Mockito.when(taskService.createTask(task)).thenReturn(task);

        mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(taskJson))
                .andExpect(status().isCreated())
                .andExpect(content().json(taskJson));

        verify(taskService, times(1)).createTask(task);
    }

    @Test
    void updateTask() throws Exception {
        String taskJson = objectMapper.writeValueAsString(task);
        Mockito.when(taskService.updateTask(1L, task)).thenReturn(task);

        mockMvc.perform(
                        put("/tasks/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(taskJson))
                .andExpect(status().isOk())
                .andExpect(content().json(taskJson));

        verify(taskService, times(1)).updateTask(1L, task);
    }

    @Test
    void startTask() throws Exception {
        mockMvc.perform(post("/tasks/{id}/start", 1L)).andExpect(status().isOk());

        verify(taskService, times(1)).startTask(1L);
    }

    @Test
    void completeTask() throws Exception {
        mockMvc.perform(post("/tasks/{id}/complete", 1L)).andExpect(status().isOk());
        verify(taskService, times(1)).completeTask(1L);
    }
}
