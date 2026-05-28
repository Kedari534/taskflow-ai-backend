package com.aitaskmanager.dto;

import com.aitaskmanager.model.Task.Priority;
import com.aitaskmanager.model.Task.Status;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private String estimatedTime;
    private LocalDate dueDate;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
