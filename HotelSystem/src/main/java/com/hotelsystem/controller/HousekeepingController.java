package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.Task;
import com.hotelsystem.entity.Room;
import com.hotelsystem.service.TaskService;
import com.hotelsystem.repository.TaskRepository;
import com.hotelsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/housekeeping")
@RequiredArgsConstructor
public class HousekeepingController {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final RoomRepository roomRepository;

    /**
     * 房务工作台 - 获取待办任务
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHousekeepingDashboard(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;

        Map<String, Object> dashboard = new HashMap<>();

        // 待清洁房间列表
        List<Room> cleaningRooms = roomRepository.findByStatus(Room.RoomStatus.CLEANING);
        List<Map<String, Object>> cleaningRoomList = cleaningRooms.stream().map(room -> {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("id", room.getId());
            roomInfo.put("roomNumber", room.getRoomNumber());
            roomInfo.put("roomType", room.getRoomType());
            roomInfo.put("status", room.getStatus());
            
            // 查找关联的清洁任务
            List<Task> tasks = taskRepository.findByRoomIdAndTypeAndStatus(
                    room.getId(), Task.TaskType.CLEANING, Task.TaskStatus.PENDING);
            if (!tasks.isEmpty()) {
                Task task = tasks.get(0);
                roomInfo.put("taskId", task.getId());
                roomInfo.put("taskTitle", task.getTitle());
                roomInfo.put("taskDescription", task.getDescription());
                roomInfo.put("priority", task.getPriority());
                roomInfo.put("dueDate", task.getDueDate());
            }
            
            return roomInfo;
        }).collect(Collectors.toList());
        dashboard.put("cleaningRooms", cleaningRoomList);

        // 待维修房间
        List<Room> maintenanceRooms = roomRepository.findByStatus(Room.RoomStatus.MAINTENANCE);
        dashboard.put("maintenanceRooms", maintenanceRooms.size());

        // 我的待办任务
        if (username != null) {
            List<Task> myTasks = taskService.getPendingTasksByAssignedTo(username);
            dashboard.put("myTasks", myTasks);
        }

        // 按优先级排序的待办任务
        List<Task> allPendingTasks = taskRepository.findByStatus(Task.TaskStatus.PENDING);
        List<Task> sortedTasks = allPendingTasks.stream()
                .sorted((t1, t2) -> Integer.compare(
                        t2.getPriority() != null ? t2.getPriority() : 0,
                        t1.getPriority() != null ? t1.getPriority() : 0))
                .collect(Collectors.toList());
        dashboard.put("allPendingTasks", sortedTasks);

        // 今日已完成任务数
        long todayCompletedCount = taskRepository.findAll().stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED &&
                        t.getCompletedAt() != null &&
                        t.getCompletedAt().toLocalDate().equals(java.time.LocalDate.now()))
                .count();
        dashboard.put("todayCompletedCount", todayCompletedCount);

        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * 获取清洁任务详情
     */
    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaskDetails(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        Map<String, Object> taskDetails = new HashMap<>();
        taskDetails.put("task", task);
        
        if (task.getRoom() != null) {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("id", task.getRoom().getId());
            roomInfo.put("roomNumber", task.getRoom().getRoomNumber());
            roomInfo.put("roomType", task.getRoom().getRoomType());
            roomInfo.put("amenities", task.getRoom().getAmenities());
            taskDetails.put("room", roomInfo);
        }

        if (task.getReservation() != null) {
            Map<String, Object> reservationInfo = new HashMap<>();
            reservationInfo.put("id", task.getReservation().getId());
            reservationInfo.put("reservationNumber", task.getReservation().getReservationNumber());
            reservationInfo.put("checkInDate", task.getReservation().getCheckInDate());
            reservationInfo.put("checkOutDate", task.getReservation().getCheckOutDate());
            reservationInfo.put("specialRequests", task.getReservation().getSpecialRequests());
            taskDetails.put("reservation", reservationInfo);
        }

        // 清洁检查清单（可以从系统配置或任务描述中获取）
        List<String> checklist = java.util.Arrays.asList(
                "更换床单被套", "清洁卫生间", "补充洗漱用品", "清洁地面", 
                "检查设施", "补充矿泉水", "检查空调", "检查电视"
        );
        taskDetails.put("checklist", checklist);

        return ResponseEntity.ok(ApiResponse.success(taskDetails));
    }

    /**
     * 开始清洁任务
     */
    @PostMapping("/task/{taskId}/start")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Task>> startTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        try {
            String assignedTo = request != null && request.containsKey("assignedTo") ?
                    request.get("assignedTo") : authentication.getName();
            
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("任务不存在"));
            
            task.setAssignedTo(assignedTo);
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            task = taskRepository.save(task);
            
            return ResponseEntity.ok(ApiResponse.success("任务已开始", task));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 完成清洁任务
     */
    @PostMapping("/task/{taskId}/complete")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Task>> completeTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            Task task = taskService.updateTaskStatus(taskId, Task.TaskStatus.COMPLETED);
            
            // 如果任务关联了房间，更新房间状态
            if (task.getRoom() != null) {
                Room room = task.getRoom();
                if (room.getStatus() == Room.RoomStatus.CLEANING) {
                    room.setStatus(Room.RoomStatus.AVAILABLE);
                    roomRepository.save(room);
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success("任务已完成", task));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 批量更新房态
     */
    @PostMapping("/rooms/batch-update-status")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUpdateRoomStatus(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> updates = (List<Map<String, Object>>) request.get("updates");
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new java.util.ArrayList<>();

            for (Map<String, Object> update : updates) {
                try {
                    Long roomId = Long.valueOf(update.get("roomId").toString());
                    Room.RoomStatus newStatus = Room.RoomStatus.valueOf(update.get("status").toString());
                    
                    Room room = roomRepository.findById(roomId)
                            .orElseThrow(() -> new RuntimeException("房间不存在"));
                    room.setStatus(newStatus);
                    roomRepository.save(room);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("房间 " + update.get("roomId") + ": " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errors", errors);
            
            return ResponseEntity.ok(ApiResponse.success("批量更新完成", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

