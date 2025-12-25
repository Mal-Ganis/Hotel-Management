package com.hotelsystem.service;

import com.hotelsystem.entity.Room;
import com.hotelsystem.entity.RoomStandardConsumption;
import com.hotelsystem.repository.RoomRepository;
import com.hotelsystem.repository.RoomStandardConsumptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomStatusUpdateService {

    private final RoomRepository roomRepository;
    private final RoomStandardConsumptionRepository standardConsumptionRepository;
    private final InventoryService inventoryService;

    /**
     * 当房间状态更新为AVAILABLE（已清洁完成）时，自动扣减标准耗材
     */
    @Transactional
    public void handleRoomCleaned(Long roomId, String createdBy) {
        Room room = roomRepository.findById(roomId)
                .orElse(null);
        
        if (room == null) {
            log.warn("房间不存在: {}", roomId);
            return;
        }

        // 获取该房间的标准耗材配置
        var standardConsumptions = standardConsumptionRepository.findByRoomId(roomId);
        
        for (RoomStandardConsumption standard : standardConsumptions) {
            try {
                com.hotelsystem.entity.Inventory inventory = standard.getInventory();
                BigDecimal quantity = BigDecimal.valueOf(standard.getStandardQuantity());
                
                // 自动出库标准耗材
                inventoryService.stockOut(
                        inventory.getId(),
                        quantity,
                        "房间清洁标准消耗",
                        "ROOM_" + room.getRoomNumber(),
                        createdBy
                );
                
                log.info("房间 {} 清洁完成，自动扣减耗材: {} x {}", 
                        room.getRoomNumber(), inventory.getItemName(), quantity);
            } catch (Exception e) {
                log.error("房间 {} 清洁时扣减耗材失败: {}", room.getRoomNumber(), e.getMessage());
                // 继续处理其他耗材，不中断流程
            }
        }
    }
}

