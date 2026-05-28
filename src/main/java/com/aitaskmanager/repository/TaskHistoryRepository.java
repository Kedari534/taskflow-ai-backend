package com.aitaskmanager.repository;

import com.aitaskmanager.model.TaskHistory;
import com.aitaskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    List<TaskHistory> findByTask(Task task);
    TaskHistory findFirstByTaskIdOrderByTimestampDesc(Long taskId);
}
