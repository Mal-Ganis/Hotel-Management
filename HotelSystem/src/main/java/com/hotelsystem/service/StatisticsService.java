package com.hotelsystem.service;

import com.hotelsystem.entity.Reservation;
import com.hotelsystem.entity.Room;
import com.hotelsystem.repository.ReservationRepository;
import com.hotelsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 统计报表服务
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

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
}

