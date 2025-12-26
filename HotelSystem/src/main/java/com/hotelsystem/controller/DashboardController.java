package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.*;
import com.hotelsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作台控制器 - 提供角色化的待办任务和数据
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final TaskRepository taskRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    /**
     * 获取角色化工作台数据
     */
    @GetMapping("/workspace")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWorkspace(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Map<String, Object> workspace = new HashMap<>();
        workspace.put("role", user.getRole().name());
        workspace.put("username", username);

        switch (user.getRole()) {
            case RECEPTIONIST:
                workspace.put("data", getReceptionistWorkspace());
                break;
            case HOUSEKEEPING:
                workspace.put("data", getHousekeepingWorkspace());
                break;
            case MANAGER:
            case ADMIN:
                workspace.put("data", getManagerWorkspace());
                break;
            default:
                workspace.put("data", Collections.emptyMap());
        }

        return ResponseEntity.ok(ApiResponse.success(workspace));
    }

    /**
     * 前台工作台数据
     */
    private Map<String, Object> getReceptionistWorkspace() {
        Map<String, Object> data = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 待办理入住
        List<Reservation> pendingCheckIns = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CONFIRMED
                        && (r.getCheckInDate().equals(today) || r.getCheckInDate().isBefore(today)))
                .sorted(Comparator.comparing(Reservation::getCheckInDate)
                .thenComparing(Reservation::getCreatedAt))
                .limit(10)
                .collect(Collectors.toList());

        // 待办理退房
        List<Reservation> pendingCheckOuts = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CHECKED_IN
                        && (r.getCheckOutDate().equals(today) || r.getCheckOutDate().isBefore(today)))
                .sorted(Comparator.comparing(Reservation::getCheckOutDate))
                .limit(10)
                .collect(Collectors.toList());

        // 待处理订单
        List<Reservation> pendingReservations = reservationRepository.findByStatus(
                Reservation.ReservationStatus.PENDING);

        // 今日统计
        long todayCheckIns = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CHECKED_IN
                        && r.getCheckInDate().equals(today))
                .count();

        long todayCheckOuts = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CHECKED_OUT
                        && r.getCheckOutDate().equals(today))
                .count();

        data.put("pendingCheckIns", pendingCheckIns.stream()
                .map(this::convertReservationToMap)
                .collect(Collectors.toList()));
        data.put("pendingCheckOuts", pendingCheckOuts.stream()
                .map(this::convertReservationToMap)
                .collect(Collectors.toList()));
        data.put("pendingReservations", pendingReservations.stream()
                .map(this::convertReservationToMap)
                .collect(Collectors.toList()));
        data.put("todayCheckIns", todayCheckIns);
        data.put("todayCheckOuts", todayCheckOuts);
        data.put("pendingCheckInCount", pendingCheckIns.size());
        data.put("pendingCheckOutCount", pendingCheckOuts.size());
        data.put("pendingReservationCount", pendingReservations.size());

        return data;
    }

    /**
     * 房务工作台数据
     */
    private Map<String, Object> getHousekeepingWorkspace() {
        Map<String, Object> data = new HashMap<>();

        // 待清洁房间
        List<Room> cleaningRooms = roomRepository.findByStatus(Room.RoomStatus.CLEANING);
        
        // 待清洁任务
        List<Task> cleaningTasks = taskRepository.findByTypeAndStatus(
                Task.TaskType.CLEANING, Task.TaskStatus.PENDING);

        // 维修中房间
        List<Room> maintenanceRooms = roomRepository.findByStatus(Room.RoomStatus.MAINTENANCE);

        // 库存预警
        List<Inventory> lowStockItems = inventoryRepository.findAll().stream()
                .filter(item -> item.getCurrentQuantity().compareTo(item.getSafetyThreshold()) < 0)
                .collect(Collectors.toList());

        data.put("cleaningRooms", cleaningRooms.stream()
                .map(this::convertRoomToMap)
                .collect(Collectors.toList()));
        data.put("cleaningTasks", cleaningTasks.stream()
                .map(this::convertTaskToMap)
                .collect(Collectors.toList()));
        data.put("maintenanceRooms", maintenanceRooms.stream()
                .map(this::convertRoomToMap)
                .collect(Collectors.toList()));
        data.put("lowStockItems", lowStockItems.stream()
                .map(this::convertInventoryToMap)
                .collect(Collectors.toList()));
        data.put("cleaningRoomCount", cleaningRooms.size());
        data.put("cleaningTaskCount", cleaningTasks.size());
        data.put("maintenanceRoomCount", maintenanceRooms.size());
        data.put("lowStockCount", lowStockItems.size());

        return data;
    }

    /**
     * 经理/管理员工作台数据
     */
    private Map<String, Object> getManagerWorkspace() {
        Map<String, Object> data = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 待审批事项（预留）
        List<Map<String, Object>> pendingApprovals = new ArrayList<>();

        // 经营数据概览
        Map<String, Object> businessOverview = new HashMap<>();
        
        // 今日收入
        List<PaymentTransaction> todayPayments = paymentTransactionRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null
                        && p.getCreatedAt().toLocalDate().equals(today)
                        && p.getType() == PaymentTransaction.TransactionType.PAYMENT
                        && p.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS)
                .collect(Collectors.toList());
        
        double todayRevenue = todayPayments.stream()
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();

        // 库存预警
        List<Inventory> lowStockItems = inventoryRepository.findAll().stream()
                .filter(item -> item.getCurrentQuantity().compareTo(item.getSafetyThreshold()) < 0)
                .collect(Collectors.toList());

        // 异常提醒（如：超时未退房、长时间未清洁等）
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // 检查超时未退房
        List<Reservation> overdueCheckOuts = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CHECKED_IN
                        && r.getCheckOutDate().isBefore(today))
                .collect(Collectors.toList());
        
        if (!overdueCheckOuts.isEmpty()) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "OVERDUE_CHECKOUT");
            alert.put("title", "超时未退房");
            alert.put("count", overdueCheckOuts.size());
            alert.put("message", String.format("有 %d 个订单超时未退房", overdueCheckOuts.size()));
            alerts.add(alert);
        }

        // 检查长时间未清洁的房间
        List<Room> longTimeCleaning = roomRepository.findByStatus(Room.RoomStatus.CLEANING).stream()
                .filter(room -> {
                    // 这里可以添加更复杂的逻辑来判断是否长时间未清洁
                    return true; // 简化处理
                })
                .collect(Collectors.toList());

        if (!longTimeCleaning.isEmpty()) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "LONG_TIME_CLEANING");
            alert.put("title", "长时间未清洁");
            alert.put("count", longTimeCleaning.size());
            alert.put("message", String.format("有 %d 个房间长时间处于清洁状态", longTimeCleaning.size()));
            alerts.add(alert);
        }

        businessOverview.put("todayRevenue", todayRevenue);
        businessOverview.put("lowStockCount", lowStockItems.size());
        businessOverview.put("alerts", alerts);

        data.put("pendingApprovals", pendingApprovals);
        data.put("businessOverview", businessOverview);
        data.put("lowStockItems", lowStockItems.stream()
                .map(this::convertInventoryToMap)
                .collect(Collectors.toList()));

        return data;
    }

    private Map<String, Object> convertReservationToMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("reservationNumber", r.getReservationNumber());
        map.put("checkInDate", r.getCheckInDate());
        map.put("checkOutDate", r.getCheckOutDate());
        map.put("status", r.getStatus().name());
        map.put("totalAmount", r.getTotalAmount());
        if (r.getGuest() != null) {
            map.put("guestName", r.getGuest().getFullName());
        }
        if (r.getRoom() != null) {
            map.put("roomNumber", r.getRoom().getRoomNumber());
        }
        return map;
    }

    private Map<String, Object> convertRoomToMap(Room r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("roomNumber", r.getRoomNumber());
        map.put("roomType", r.getRoomType());
        map.put("status", r.getStatus().name());
        map.put("price", r.getPrice());
        return map;
    }

    private Map<String, Object> convertTaskToMap(Task t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("title", t.getTitle());
        map.put("description", t.getDescription());
        map.put("type", t.getType().name());
        map.put("status", t.getStatus().name());
        map.put("priority", t.getPriority());
        map.put("dueDate", t.getDueDate());
        if (t.getRoom() != null) {
            map.put("roomNumber", t.getRoom().getRoomNumber());
        }
        return map;
    }

    private Map<String, Object> convertInventoryToMap(Inventory i) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", i.getId());
        map.put("itemName", i.getItemName());
        map.put("currentQuantity", i.getCurrentQuantity());
        map.put("safetyThreshold", i.getSafetyThreshold());
        map.put("unit", i.getUnit());
        return map;
    }
}

