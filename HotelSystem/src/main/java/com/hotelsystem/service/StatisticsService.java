package com.hotelsystem.service;

import com.hotelsystem.entity.Reservation;
import com.hotelsystem.entity.Room;
import com.hotelsystem.entity.InventoryTransaction;
import com.hotelsystem.entity.PosConsumption;
import com.hotelsystem.repository.ReservationRepository;
import com.hotelsystem.repository.RoomRepository;
import com.hotelsystem.repository.InventoryTransactionRepository;
import com.hotelsystem.repository.PosConsumptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.RoundingMode;

/**
 * 统计报表服务
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PosConsumptionRepository posConsumptionRepository;

    /**
     * 获取今日统计
     */
    public Map<String, Object> getTodayStatistics() {
        LocalDate today = LocalDate.now();

        Map<String, Object> stats = new HashMap<>();
        
        // 今日入住（简化处理：统计今日入住的预订）
        long todayCheckIns = reservationRepository.findByCheckInDateBetween(today, today).stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CHECKED_IN)
                .count();
        stats.put("todayCheckIns", todayCheckIns);

        // 今日退房（简化处理：统计今日退房的预订）
        long todayCheckOuts = reservationRepository.findByCheckOutDateBetween(today, today).stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CHECKED_OUT)
                .count();
        stats.put("todayCheckOuts", todayCheckOuts);

        // 今日新增预订（简化处理：统计今日创建的预订）
        long todayNewReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && 
                           r.getCreatedAt().toLocalDate().equals(today))
                .count();
        stats.put("todayNewReservations", todayNewReservations);

        // 今日收入（简化处理）
        BigDecimal todayRevenue = reservationRepository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && 
                           r.getCreatedAt().toLocalDate().equals(today) &&
                           r.getPaidAmount() != null)
                .map(Reservation::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayRevenue", todayRevenue);

        // 房间统计
        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.findByStatus(Room.RoomStatus.AVAILABLE).size();
        long occupiedRooms = roomRepository.findByStatus(Room.RoomStatus.OCCUPIED).size();
        long reservedRooms = roomRepository.findByStatus(Room.RoomStatus.RESERVED).size();
        
        stats.put("totalRooms", totalRooms);
        stats.put("availableRooms", availableRooms);
        stats.put("occupiedRooms", occupiedRooms);
        stats.put("reservedRooms", reservedRooms);
        stats.put("occupancyRate", totalRooms > 0 ? 
                (double)(occupiedRooms + reservedRooms) / totalRooms * 100 : 0);

        return stats;
    }

    /**
     * 获取日期范围统计
     */
    public Map<String, Object> getDateRangeStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // 预订统计
        long totalReservations = reservationRepository.findByCheckInDateBetween(startDate, endDate).size();
        stats.put("totalReservations", totalReservations);

        // 收入统计（简化处理）
        BigDecimal totalRevenue = reservationRepository.findByCheckInDateBetween(startDate, endDate).stream()
                .filter(r -> r.getPaidAmount() != null)
                .map(Reservation::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        // 按状态统计
        Map<String, Long> statusStats = new HashMap<>();
        for (Reservation.ReservationStatus status : Reservation.ReservationStatus.values()) {
            long count = reservationRepository.findByCheckInDateBetween(startDate, endDate).stream()
                    .filter(r -> r.getStatus() == status)
                    .count();
            statusStats.put(status.name(), count);
        }
        stats.put("statusStatistics", statusStats);

        return stats;
    }

    /**
     * 获取房型统计
     */
    public Map<String, Object> getRoomTypeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 按房型统计房间数量
        Map<String, Long> roomTypeCounts = new HashMap<>();
        roomRepository.findAll().forEach(room -> {
            roomTypeCounts.merge(room.getRoomType(), 1L, Long::sum);
        });
        stats.put("roomTypeCounts", roomTypeCounts);

        return stats;
    }

    /**
     * 获取完整的经营分析报表（收入、成本、利润）
     */
    public Map<String, Object> getBusinessAnalysis(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> analysis = new HashMap<>();
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // ========== 收入侧分析 ==========
        Map<String, Object> revenue = new HashMap<>();
        
        // 客房收入（已确认和已入住的预订）
        BigDecimal roomRevenue = reservationRepository.findAll().stream()
                .filter(r -> {
                    LocalDate checkIn = r.getCheckInDate();
                    return checkIn != null && 
                           !checkIn.isBefore(startDate) && 
                           !checkIn.isAfter(endDate) &&
                           (r.getStatus() == Reservation.ReservationStatus.CONFIRMED ||
                            r.getStatus() == Reservation.ReservationStatus.CHECKED_IN ||
                            r.getStatus() == Reservation.ReservationStatus.CHECKED_OUT);
                })
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        revenue.put("roomRevenue", roomRevenue);

        // POS消费收入
        BigDecimal posRevenue = posConsumptionRepository.findByConsumptionDateBetween(startDateTime, endDateTime).stream()
                .map(PosConsumption::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        revenue.put("posRevenue", posRevenue);

        // 总营收
        BigDecimal totalRevenue = roomRevenue.add(posRevenue);
        revenue.put("totalRevenue", totalRevenue);

        // 平均房价
        long checkedOutCount = reservationRepository.findAll().stream()
                .filter(r -> {
                    LocalDate checkIn = r.getCheckInDate();
                    return checkIn != null && 
                           !checkIn.isBefore(startDate) && 
                           !checkIn.isAfter(endDate) &&
                           r.getStatus() == Reservation.ReservationStatus.CHECKED_OUT;
                })
                .count();
        BigDecimal avgRoomPrice = checkedOutCount > 0 ? 
                roomRevenue.divide(BigDecimal.valueOf(checkedOutCount), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        revenue.put("avgRoomPrice", avgRoomPrice);

        // 入住率
        long totalRooms = roomRepository.count();
        long occupiedDays = reservationRepository.findAll().stream()
                .filter(r -> {
                    LocalDate checkIn = r.getCheckInDate();
                    LocalDate checkOut = r.getCheckOutDate();
                    return checkIn != null && checkOut != null &&
                           !checkOut.isBefore(startDate) && 
                           !checkIn.isAfter(endDate) &&
                           (r.getStatus() == Reservation.ReservationStatus.CHECKED_IN ||
                            r.getStatus() == Reservation.ReservationStatus.CHECKED_OUT);
                })
                .mapToLong(r -> {
                    LocalDate start = r.getCheckInDate().isBefore(startDate) ? startDate : r.getCheckInDate();
                    LocalDate end = r.getCheckOutDate().isAfter(endDate) ? endDate : r.getCheckOutDate();
                    return java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                })
                .sum();
        long totalAvailableDays = totalRooms * (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double occupancyRate = totalAvailableDays > 0 ? (double) occupiedDays / totalAvailableDays * 100 : 0;
        revenue.put("occupancyRate", occupancyRate);

        // 渠道来源占比（简化：按预订来源统计）
        Map<String, Long> channelStats = reservationRepository.findAll().stream()
                .filter(r -> {
                    LocalDate checkIn = r.getCheckInDate();
                    return checkIn != null && 
                           !checkIn.isBefore(startDate) && 
                           !checkIn.isAfter(endDate);
                })
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedBy() != null && r.getCreatedBy().startsWith("OTA") ? "OTA" : "官网/小程序",
                        Collectors.counting()
                ));
        revenue.put("channelDistribution", channelStats);

        analysis.put("revenue", revenue);

        // ========== 成本侧分析 ==========
        Map<String, Object> cost = new HashMap<>();
        
        // 物料消耗成本（出库成本）
        BigDecimal materialCost = inventoryTransactionRepository.findByType(InventoryTransaction.TransactionType.OUT).stream()
                .filter(t -> {
                    LocalDateTime createdAt = t.getCreatedAt();
                    return createdAt != null && 
                           !createdAt.isBefore(startDateTime) && 
                           !createdAt.isAfter(endDateTime);
                })
                .map(InventoryTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cost.put("materialCost", materialCost);

        // 清洁运维成本（简化：按清洁任务数量估算，实际应从任务表统计）
        // 这里简化为物料成本的30%
        BigDecimal cleaningCost = materialCost.multiply(new BigDecimal("0.3"));
        cost.put("cleaningCost", cleaningCost);

        // 平台佣金（简化：OTA订单的10%）
        BigDecimal platformCommission = channelStats.getOrDefault("OTA", 0L) > 0 ?
                roomRevenue.multiply(new BigDecimal("0.1")) : BigDecimal.ZERO;
        cost.put("platformCommission", platformCommission);

        // 总成本
        BigDecimal totalCost = materialCost.add(cleaningCost).add(platformCommission);
        cost.put("totalCost", totalCost);

        analysis.put("cost", cost);

        // ========== 利润侧分析 ==========
        Map<String, Object> profit = new HashMap<>();
        
        // 毛利润（收入 - 直接成本）
        BigDecimal grossProfit = totalRevenue.subtract(materialCost.add(cleaningCost));
        profit.put("grossProfit", grossProfit);
        profit.put("grossProfitRate", totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO);

        // 净利润（毛利润 - 固定摊销，简化处理）
        BigDecimal fixedCost = platformCommission; // 简化：只计算平台佣金
        BigDecimal netProfit = grossProfit.subtract(fixedCost);
        profit.put("netProfit", netProfit);
        profit.put("netProfitRate", totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                netProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO);

        analysis.put("profit", profit);

        // 汇总信息
        analysis.put("startDate", startDate);
        analysis.put("endDate", endDate);
        analysis.put("totalReservations", checkedOutCount);

        return analysis;
    }
    
    /**
     * 对比分析（同比、环比）
     */
    public Map<String, Object> getCompareAnalysis(LocalDate startDate, LocalDate endDate, String compareType) {
        Map<String, Object> comparison = new HashMap<>();
        
        // 当前期间数据
        Map<String, Object> currentPeriod = getBusinessAnalysis(startDate, endDate);
        comparison.put("currentPeriod", currentPeriod);
        
        // 对比期间数据
        Map<String, Object> comparePeriod = new HashMap<>();
        LocalDate compareStartDate;
        LocalDate compareEndDate;
        
        if ("YEAR_OVER_YEAR".equals(compareType)) {
            // 同比：去年同期
            compareStartDate = startDate.minusYears(1);
            compareEndDate = endDate.minusYears(1);
        } else if ("MONTH_OVER_MONTH".equals(compareType)) {
            // 环比：上个月同期
            compareStartDate = startDate.minusMonths(1);
            compareEndDate = endDate.minusMonths(1);
        } else {
            // 默认同比
            compareStartDate = startDate.minusYears(1);
            compareEndDate = endDate.minusYears(1);
        }
        
        comparePeriod = getBusinessAnalysis(compareStartDate, compareEndDate);
        comparison.put("comparePeriod", comparePeriod);
        
        // 计算增长率
        Map<String, Object> growth = new HashMap<>();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, Object> currentRevenueMap = (Map) currentPeriod.get("revenue");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, Object> compareRevenueMap = (Map) comparePeriod.get("revenue");
        java.math.BigDecimal currentRevenue = (java.math.BigDecimal) currentRevenueMap.get("totalRevenue");
        java.math.BigDecimal compareRevenue = (java.math.BigDecimal) compareRevenueMap.get("totalRevenue");
        
        if (compareRevenue.compareTo(BigDecimal.ZERO) > 0) {
            java.math.BigDecimal revenueGrowth = currentRevenue.subtract(compareRevenue)
                    .divide(compareRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            growth.put("revenueGrowth", revenueGrowth);
        } else {
            growth.put("revenueGrowth", BigDecimal.ZERO);
        }
        
        comparison.put("growth", growth);
        comparison.put("compareType", compareType);
        
        return comparison;
    }
    
    /**
     * 实时数据看板
     */
    public Map<String, Object> getRealTimeDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        // 今日实时收入
        java.math.BigDecimal todayRevenue = reservationRepository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && 
                           r.getCreatedAt().toLocalDate().equals(today) &&
                           r.getPaidAmount() != null)
                .map(Reservation::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.put("todayRevenue", todayRevenue);
        
        // 当前入住率
        long totalRooms = roomRepository.count();
        long occupiedRooms = roomRepository.findByStatus(Room.RoomStatus.OCCUPIED).size();
        long reservedRooms = roomRepository.findByStatus(Room.RoomStatus.RESERVED).size();
        double currentOccupancyRate = totalRooms > 0 ? 
                (double)(occupiedRooms + reservedRooms) / totalRooms * 100 : 0;
        dashboard.put("currentOccupancyRate", currentOccupancyRate);
        
        // 待处理任务数量
        // 这里简化处理，实际应从TaskRepository查询
        dashboard.put("pendingTasks", 0);
        
        // 库存预警信息
        long lowStockCount = inventoryTransactionRepository.findAll().stream()
                .map(t -> t.getInventory())
                .filter(inv -> inv != null && 
                        inv.getCurrentQuantity().compareTo(inv.getSafetyThreshold()) <= 0)
                .distinct()
                .count();
        dashboard.put("lowStockCount", lowStockCount);
        
        // 今日新增预订
        long todayNewReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && 
                           r.getCreatedAt().toLocalDate().equals(today))
                .count();
        dashboard.put("todayNewReservations", todayNewReservations);
        
        // 今日入住数
        long todayCheckIns = reservationRepository.findByCheckInDateBetween(today, today).stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CHECKED_IN)
                .count();
        dashboard.put("todayCheckIns", todayCheckIns);
        
        // 今日退房数
        long todayCheckOuts = reservationRepository.findByCheckOutDateBetween(today, today).stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CHECKED_OUT)
                .count();
        dashboard.put("todayCheckOuts", todayCheckOuts);
        
        dashboard.put("lastUpdated", LocalDateTime.now());
        
        return dashboard;
    }
    
    /**
     * 导出数据为Excel
     */
    public org.springframework.core.io.Resource exportToExcel(LocalDate startDate, LocalDate endDate, String exportType) throws Exception {
        Map<String, Object> data;
        
        if ("BUSINESS_ANALYSIS".equals(exportType)) {
            data = getBusinessAnalysis(startDate, endDate);
        } else {
            data = getDateRangeStatistics(startDate, endDate);
        }
        
        // 创建Excel工作簿
        org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("经营分析报表");
        
        // 创建标题行
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("项目");
        headerRow.createCell(1).setCellValue("数值");
        
        int rowNum = 1;
        
        // 写入收入数据
        if (data.containsKey("revenue")) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Map<String, Object> revenue = (Map) data.get("revenue");
            org.apache.poi.ss.usermodel.Row revenueHeader = sheet.createRow(rowNum++);
            revenueHeader.createCell(0).setCellValue("收入分析");
            revenueHeader.getCell(0).setCellStyle(createHeaderStyle(workbook));
            
            for (Map.Entry<String, Object> entry : revenue.entrySet()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                if (entry.getValue() instanceof BigDecimal) {
                    row.createCell(1).setCellValue(((BigDecimal) entry.getValue()).doubleValue());
                } else {
                    row.createCell(1).setCellValue(entry.getValue().toString());
                }
            }
        }
        
        // 写入成本数据
        if (data.containsKey("cost")) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Map<String, Object> cost = (Map) data.get("cost");
            rowNum++;
            org.apache.poi.ss.usermodel.Row costHeader = sheet.createRow(rowNum++);
            costHeader.createCell(0).setCellValue("成本分析");
            costHeader.getCell(0).setCellStyle(createHeaderStyle(workbook));
            
            for (Map.Entry<String, Object> entry : cost.entrySet()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                if (entry.getValue() instanceof BigDecimal) {
                    row.createCell(1).setCellValue(((BigDecimal) entry.getValue()).doubleValue());
                } else {
                    row.createCell(1).setCellValue(entry.getValue().toString());
                }
            }
        }
        
        // 写入利润数据
        if (data.containsKey("profit")) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Map<String, Object> profit = (Map) data.get("profit");
            rowNum++;
            org.apache.poi.ss.usermodel.Row profitHeader = sheet.createRow(rowNum++);
            profitHeader.createCell(0).setCellValue("利润分析");
            profitHeader.getCell(0).setCellStyle(createHeaderStyle(workbook));
            
            for (Map.Entry<String, Object> entry : profit.entrySet()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                if (entry.getValue() instanceof BigDecimal) {
                    row.createCell(1).setCellValue(((BigDecimal) entry.getValue()).doubleValue());
                } else {
                    row.createCell(1).setCellValue(entry.getValue().toString());
                }
            }
        }
        
        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        
        // 写入临时文件
        java.io.File tempFile = java.io.File.createTempFile("statistics_", ".xlsx");
        try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
            workbook.write(outputStream);
        }
        workbook.close();
        
        return new org.springframework.core.io.FileSystemResource(tempFile);
    }
    
    private org.apache.poi.ss.usermodel.CellStyle createHeaderStyle(org.apache.poi.xssf.usermodel.XSSFWorkbook workbook) {
        org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}

