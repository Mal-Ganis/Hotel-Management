package com.hotelsystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckInRequest {
    // 可选：前台收取的补交金额
    private BigDecimal collectAmount;
}
