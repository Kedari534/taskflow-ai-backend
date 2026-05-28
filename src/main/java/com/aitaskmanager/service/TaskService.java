package com.aitaskmanager.service;

import com.aitaskmanager.dto.TaskDTO;
import com.aitaskmanager.dto.TaskHistoryDTO;
import com.aitaskmanager.model.Task;
import com.aitaskmanager.model.TaskHistory;
import com.aitaskmanager.model.User;
import com.aitaskmanager.repository.TaskHistoryRepository;
import com.aitaskmanager.repository.TaskRepository;
import com.aitaskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {
    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TaskHistoryRepository historyRepository;

    public List<TaskDTO> getAllTasks(Task.Status status, Task.Priority priority, String category, String search) {
        User user = getCurrentUser();
        return taskRepository.findByFilters(user, status, priority, category, search)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        User user = getCurrentUser();
        Task task = Task.builder()
                .title(taskDTO.getTitle())
                .description(taskDTO.getDescription())
                .priority(taskDTO.getPriority())
                .status(taskDTO.getStatus() != null ? taskDTO.getStatus() : Task.Status.PENDING)
                .estimatedTime(taskDTO.getEstimatedTime())
                .dueDate(taskDTO.getDueDate())
                .category(taskDTO.getCategory())
                .user(user)
                .build();

        Task savedTask = taskRepository.save(task);
        logHistory(savedTask, "CREATED");
        return convertToDTO(savedTask);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setPriority(taskDTO.getPriority());
        task.setStatus(taskDTO.getStatus());
        task.setEstimatedTime(taskDTO.getEstimatedTime());
        task.setDueDate(taskDTO.getDueDate());
        task.setCategory(taskDTO.getCategory());

        Task updatedTask = taskRepository.save(task);
        logHistory(updatedTask, "UPDATED");
        return convertToDTO(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public List<TaskHistoryDTO> getTaskHistory(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        return historyRepository.findByTask(task).stream()
                .map(history -> TaskHistoryDTO.builder()
                        .id(history.getId())
                        .action(history.getAction())
                        .stateHash(history.getStateHash())
                        .previousHash(history.getPreviousHash())
                        .timestamp(history.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }

    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setPriority(task.getPriority());
        dto.setStatus(task.getStatus());
        dto.setEstimatedTime(task.getEstimatedTime());
        dto.setDueDate(task.getDueDate());
        dto.setCategory(task.getCategory());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }

    private void logHistory(Task task, String action) {
        String state = task.getTitle() + task.getStatus() + task.getPriority();
        String currentHash = hashString(state);
        
        TaskHistory lastHistory = historyRepository.findFirstByTaskIdOrderByTimestampDesc(task.getId());
        String previousHash = (lastHistory != null) ? lastHistory.getStateHash() : "0";

        TaskHistory history = TaskHistory.builder()
                .task(task)
                .action(action)
                .stateHash(currentHash)
                .previousHash(previousHash)
                .build();
        
        historyRepository.save(history);
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "hash_error";
        }
    }
}
