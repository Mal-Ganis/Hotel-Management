package com.hotelsystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckInRequest {
    // 可选：前台收取的补交金额
    private BigDecimal collectAmount;
    
    // 散客快速入住：宾客信息（如果未提供reservationId）
    private Long guestId;
    private String idCardNumber; // 身份证号（用于快速查找或创建宾客）
    private Long roomId;
    private java.time.LocalDate checkInDate;
    private java.time.LocalDate checkOutDate;
    private Integer numberOfGuests;
    private String preferredRoomType;
    
    // 确认信息
    private Boolean confirmed; // 是否已确认
}
