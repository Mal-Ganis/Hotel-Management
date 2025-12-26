package com.hotelsystem.service;

import com.hotelsystem.entity.MaintenanceOrder;
import com.hotelsystem.entity.Room;
import com.hotelsystem.repository.MaintenanceOrderRepository;
import com.hotelsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceOrderRepository maintenanceOrderRepository;
    private final RoomRepository roomRepository;

    public List<MaintenanceOrder> getAllMaintenanceOrders() {
        return maintenanceOrderRepository.findAll();
    }

    public List<MaintenanceOrder> getPendingMaintenanceOrders() {
        return maintenanceOrderRepository.findByStatus(MaintenanceOrder.MaintenanceStatus.PENDING);
    }

    public List<MaintenanceOrder> getMaintenanceOrdersByRoom(Long roomId) {
        return maintenanceOrderRepository.findByRoomId(roomId);
    }

    public List<MaintenanceOrder> getMaintenanceOrdersByAssignedTo(String assignedTo) {
        return maintenanceOrderRepository.findByAssignedTo(assignedTo);
    }

    public Optional<MaintenanceOrder> getMaintenanceOrderById(Long id) {
        return maintenanceOrderRepository.findById(id);
    }

    @Transactional
    public MaintenanceOrder createMaintenanceOrder(Long roomId, String problemDescription, 
                                                   MaintenanceOrder.UrgencyLevel urgency, 
                                                   LocalDateTime estimatedCompletionTime,
                                                   String createdBy) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        MaintenanceOrder order = new MaintenanceOrder();
        order.setRoom(room);
        order.setProblemDescription(problemDescription);
        order.setUrgency(urgency != null ? urgency : MaintenanceOrder.UrgencyLevel.MEDIUM);
        order.setEstimatedCompletionTime(estimatedCompletionTime);
        order.setStatus(MaintenanceOrder.MaintenanceStatus.PENDING);
        order.setCreatedBy(createdBy);

        // 更新房间状态为维修中
        room.setStatus(Room.RoomStatus.MAINTENANCE);
        roomRepository.save(room);

        return maintenanceOrderRepository.save(order);
    }

    @Transactional
    public MaintenanceOrder assignMaintenanceOrder(Long orderId, String assignedTo) {
        MaintenanceOrder order = maintenanceOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修工单不存在"));

        order.setAssignedTo(assignedTo);
        order.setStatus(MaintenanceOrder.MaintenanceStatus.IN_PROGRESS);
        return maintenanceOrderRepository.save(order);
    }

    @Transactional
    public MaintenanceOrder completeMaintenanceOrder(Long orderId, java.math.BigDecimal cost, String notes) {
        MaintenanceOrder order = maintenanceOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修工单不存在"));

        order.setStatus(MaintenanceOrder.MaintenanceStatus.COMPLETED);
        order.setActualCompletionTime(LocalDateTime.now());
        order.setCost(cost);
        order.setNotes(notes);

        // 更新房间状态为空闲
        Room room = order.getRoom();
        if (room != null) {
            room.setStatus(Room.RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        return maintenanceOrderRepository.save(order);
    }

    @Transactional
    public void cancelMaintenanceOrder(Long orderId) {
        MaintenanceOrder order = maintenanceOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修工单不存在"));

        order.setStatus(MaintenanceOrder.MaintenanceStatus.CANCELLED);

        // 更新房间状态为空闲
        Room room = order.getRoom();
        if (room != null) {
            room.setStatus(Room.RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        maintenanceOrderRepository.save(order);
    }
}

