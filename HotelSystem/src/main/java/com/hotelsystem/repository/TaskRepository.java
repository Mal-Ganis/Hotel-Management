package com.hotelsystem.repository;

import com.hotelsystem.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedTo(String assignedTo);
    List<Task> findByAssignedToAndStatus(String assignedTo, Task.TaskStatus status);
    List<Task> findByType(Task.TaskType type);
    List<Task> findByStatus(Task.TaskStatus status);
    List<Task> findByDueDateBeforeAndStatus(LocalDateTime date, Task.TaskStatus status);
}

