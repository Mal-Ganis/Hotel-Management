package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.User;
import com.hotelsystem.repository.ReservationRepository;
import com.hotelsystem.repository.RoomRepository;
import com.hotelsystem.repository.TaskRepository;
import com.hotelsystem.repository.InventoryRepository;
import com.hotelsystem.entity.Reservation;
import com.hotelsystem.entity.Room;
import com.hotelsystem.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final TaskRepository taskRepository;
    private final InventoryRepository inventoryRepository;
    private final com.hotelsystem.repository.UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTodos(Authentication authentication) {
        String username = authentication.getName();
        User.UserRole role = userRepository.findByUsername(username)
                .map(User::getRole)
                .orElse(null);

        if (role == null) {
            return ResponseEntity.ok(ApiResponse.error("用户不存在"));
        }

        List<Map<String, Object>> todos = new ArrayList<>();

        // 根据角色返回不同的待办事项
        switch (role) {
            case RECEPTIONIST:
                // 前台：新订单
                List<Reservation> newReservations = reservationRepository.findByStatus(Reservation.ReservationStatus.PENDING);
                if (!newReservations.isEmpty()) {
                    Map<String, Object> todo = new HashMap<>();
                    todo.put("type", "NEW_ORDER");
                    todo.put("title", "有新的订单，请查看");
                    todo.put("count", newReservations.size());
                    todo.put("message", String.format("有 %d 个新订单待处理", newReservations.size()));
                    todos.add(todo);
                }
                break;

            case HOUSEKEEPING:
                // 房务：待清洁房间
                List<Room> cleaningRooms = roomRepository.findByStatus(Room.RoomStatus.CLEANING);
                List<Task> cleaningTasks = taskRepository.findByTypeAndStatus(
                        Task.TaskType.CLEANING, Task.TaskStatus.PENDING);
                int cleaningCount = Math.max(cleaningRooms.size(), cleaningTasks.size());
                if (cleaningCount > 0) {
                    Map<String, Object> todo = new HashMap<>();
                    todo.put("type", "CLEANING_ROOM");
                    todo.put("title", "有新的房间待清洁");
                    todo.put("count", cleaningCount);
                    todo.put("message", String.format("有 %d 个房间待清洁", cleaningCount));
                    todos.add(todo);
                }
                break;

            case MANAGER:
            case ADMIN:
                // 经营者/管理员：库存不足
                List<com.hotelsystem.entity.Inventory> lowStockItems = inventoryRepository.findAll().stream()
                        .filter(item -> item.getCurrentQuantity().compareTo(item.getSafetyThreshold()) < 0)
                        .toList();
                if (!lowStockItems.isEmpty()) {
                    Map<String, Object> todo = new HashMap<>();
                    todo.put("type", "LOW_STOCK");
                    todo.put("title", "库存不足");
                    todo.put("count", lowStockItems.size());
                    StringBuilder items = new StringBuilder();
                    for (int i = 0; i < Math.min(3, lowStockItems.size()); i++) {
                        if (i > 0) items.append("、");
                        items.append(lowStockItems.get(i).getItemName());
                    }
                    if (lowStockItems.size() > 3) {
                        items.append("等");
                    }
                    todo.put("message", String.format("%s库存不足", items.toString()));
                    todos.add(todo);
                }
                break;
        }

        return ResponseEntity.ok(ApiResponse.success(todos));
    }
}

