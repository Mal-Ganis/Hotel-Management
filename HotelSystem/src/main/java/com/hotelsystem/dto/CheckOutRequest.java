package com.hotelsystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckOutRequest {
    // 房费外的额外消费
    private BigDecimal extraCharges;

    // 前台收取的补交金额（可用于补足差额）
    private BigDecimal collectAmount;
}
