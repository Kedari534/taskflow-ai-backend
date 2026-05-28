package com.aitaskmanager.repository;

import com.aitaskmanager.model.Task;
import com.aitaskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
    
    @Query("SELECT t FROM Task t WHERE t.user = :user AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:category IS NULL OR t.category = :category) AND " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Task> findByFilters(@Param("user") User user, 
                           @Param("status") Task.Status status, 
                           @Param("priority") Task.Priority priority, 
                           @Param("category") String category,
                           @Param("search") String search);

    Long countByStatus(Task.Status status);
}
