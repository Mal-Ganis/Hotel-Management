package com.hotelsystem.service;

import com.hotelsystem.entity.Task;
import com.hotelsystem.entity.Room;
import com.hotelsystem.entity.Reservation;
import com.hotelsystem.repository.TaskRepository;
import com.hotelsystem.repository.RoomRepository;
import com.hotelsystem.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public List<Task> getTasksByAssignedTo(String assignedTo) {
        return taskRepository.findByAssignedTo(assignedTo);
    }

    public List<Task> getPendingTasksByAssignedTo(String assignedTo) {
        return taskRepository.findByAssignedToAndStatus(assignedTo, Task.TaskStatus.PENDING);
    }

    public Task createTask(String title, String description, Task.TaskType type, 
                          Long roomId, Long reservationId, String assignedTo, 
                          Integer priority, LocalDateTime dueDate, String createdBy) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setType(type);
        task.setAssignedTo(assignedTo);
        task.setPriority(priority != null ? priority : 5);
        task.setDueDate(dueDate);
        task.setCreatedBy(createdBy);
        task.setStatus(Task.TaskStatus.PENDING);

        if (roomId != null) {
            Room room = roomRepository.findById(roomId)
                    .orElse(null);
            task.setRoom(room);
        }

        if (reservationId != null) {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElse(null);
            task.setReservation(reservation);
        }

        return taskRepository.save(task);
    }

    public Task updateTaskStatus(Long taskId, Task.TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        
        task.setStatus(status);
        if (status == Task.TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }
}

