package com.hotelsystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckOutRequest {
    // 房费外的额外消费
    private BigDecimal extraCharges;

    // 前台收取的补交金额（可用于补足差额）
    private BigDecimal collectAmount;
    
    // 房间检查信息
    private Boolean roomInspectionCompleted; // 房间检查是否完成
    private Boolean hasDamage; // 是否有损坏
    private String damageDescription; // 损坏描述
    private Boolean itemsLeftBehind; // 是否有遗留物品
    private String itemsDescription; // 遗留物品描述
}
