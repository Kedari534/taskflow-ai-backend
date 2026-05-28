package com.aitaskmanager.repository;

import com.aitaskmanager.model.AILog;
import com.aitaskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AILogRepository extends JpaRepository<AILog, Long> {
    List<AILog> findByUser(User user);
}
