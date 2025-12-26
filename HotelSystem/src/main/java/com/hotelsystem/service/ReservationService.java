package com.hotelsystem.service;

import com.hotelsystem.dto.ReservationDto;
import com.hotelsystem.entity.Guest;
import com.hotelsystem.entity.Reservation;
import com.hotelsystem.entity.Room;
import com.hotelsystem.entity.Task;
import com.hotelsystem.entity.ReservationHistory;
import com.hotelsystem.entity.RoomCheckInspection;
import com.hotelsystem.repository.GuestRepository;
import com.hotelsystem.repository.ReservationRepository;
import com.hotelsystem.repository.RoomRepository;
import com.hotelsystem.repository.ReservationHistoryRepository;
import com.hotelsystem.repository.RoomCheckInspectionRepository;
import com.hotelsystem.repository.SystemSettingRepository;
import com.hotelsystem.entity.SystemSetting;
import com.hotelsystem.dto.CheckOutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import org.springframework.scheduling.annotation.Scheduled;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;
    private final TaskService taskService;
    private final com.hotelsystem.service.PaymentService paymentService;
    private final ReservationHistoryRepository reservationHistoryRepository;
    private final RoomCheckInspectionRepository roomCheckInspectionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final com.hotelsystem.repository.PosConsumptionRepository posConsumptionRepository;
    private final com.hotelsystem.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_ASSIGN_RETRIES = 3;
    
    // 获取保证金比例（从系统配置读取，默认30%）
    private BigDecimal getDepositRate() {
        Optional<SystemSetting> setting = systemSettingRepository.findByKey("deposit_rate");
        if (setting.isPresent()) {
            try {
                return new BigDecimal(setting.get().getValue());
            } catch (Exception e) {
                return new BigDecimal("0.30"); // 默认30%
            }
        }
        return new BigDecimal("0.30"); // 默认30%
    }
    
    // 记录预订历史
    private void recordHistory(Long reservationId, String action, String oldValue, String newValue, String description, String operator) {
        ReservationHistory history = new ReservationHistory();
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation != null) {
            history.setReservation(reservation);
            history.setAction(action);
            history.setOldValue(oldValue);
            history.setNewValue(newValue);
            history.setDescription(description);
            history.setOperator(operator != null ? operator : "系统");
            reservationHistoryRepository.save(history);
        }
    }

    public List<ReservationDto> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(ReservationDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<ReservationDto> getReservationById(Long id) {
        return reservationRepository.findById(id)
                .map(ReservationDto::fromEntity);
    }

    @Transactional
    public ReservationDto createReservation(ReservationDto reservationDto) {
        int attempts = 0;
        while (true) {
            try {
                // 验证宾客是否存在
                Guest guest = guestRepository.findById(reservationDto.getGuestId())
                        .orElseThrow(() -> new RuntimeException("宾客不存在"));

                // 尝试确定房间：优先使用 roomId；若未提供则按 preferredRoomType 自动分配
                Room room = null;
                if (reservationDto.getRoomId() != null) {
                    room = roomRepository.findById(reservationDto.getRoomId())
                            .orElseThrow(() -> new RuntimeException("房间不存在"));
                    if (!isRoomAvailable(room.getId(), reservationDto.getCheckInDate(), reservationDto.getCheckOutDate())) {
                        throw new RuntimeException("房间在指定日期不可用");
                    }
                } else if (reservationDto.getPreferredRoomType() != null && !reservationDto.getPreferredRoomType().isBlank()) {
                    room = findAvailableRoomByType(reservationDto.getPreferredRoomType(), reservationDto.getCheckInDate(), reservationDto.getCheckOutDate());
                    if (room == null) {
                        throw new RuntimeException("未找到可用的" + reservationDto.getPreferredRoomType() + "房间");
                    }
                }

                Reservation reservation = reservationDto.toEntity();
                reservation.setGuest(guest);
                reservation.setRoom(room);

                // 计算总金额（简化版：天数 × 房间价格）
                long days = reservationDto.getCheckInDate().until(reservationDto.getCheckOutDate()).getDays();
                if (days <= 0) {
                    throw new RuntimeException("离店日期必须晚于入住日期");
                }

                if (reservation.getTotalAmount() == null) {
                    BigDecimal totalAmount = room.getPrice().multiply(BigDecimal.valueOf(days));
                    reservation.setTotalAmount(totalAmount);
                }

                // 新建预订时设为 PENDING 状态，房间暂不锁定
                // 只有在支付成功后才会变为 CONFIRMED 并锁定房间
                reservation.setStatus(Reservation.ReservationStatus.PENDING);

                Reservation savedReservation = reservationRepository.save(reservation);
                
                // 记录创建历史
                recordHistory(savedReservation.getId(), "CREATED", null, savedReservation.getStatus().toString(), 
                    "创建预订", savedReservation.getCreatedBy());
                
                return ReservationDto.fromEntity(savedReservation);
            } catch (OptimisticLockingFailureException ex) {
                attempts++;
                if (attempts >= MAX_ASSIGN_RETRIES) {
                    throw new RuntimeException("房间分配冲突，稍后重试");
                }
                // 重试分配
            }
        }
    }

    @Transactional
    public ReservationDto updateReservation(Long id, ReservationDto reservationDto) {
        int attempts = 0;
        while (true) {
            try {
                Reservation existingReservation = reservationRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("预订不存在"));

                // 验证宾客是否存在
                Guest guest = guestRepository.findById(reservationDto.getGuestId())
                        .orElseThrow(() -> new RuntimeException("宾客不存在"));
                // 如果提供 roomId 则验证并使用；否则若提供 preferredRoomType 且当前未分配房间，则尝试分配
                Room room = null;
                if (reservationDto.getRoomId() != null) {
                    room = roomRepository.findById(reservationDto.getRoomId())
                            .orElseThrow(() -> new RuntimeException("房间不存在"));
                    if (!isRoomAvailable(room.getId(), reservationDto.getCheckInDate(), reservationDto.getCheckOutDate(), id)) {
                        throw new RuntimeException("房间在指定日期不可用");
                    }
                    existingReservation.setRoom(room);
                    // 注意：房间状态在付款成功时才设为 RESERVED（见 PaymentService.markSuccess）
                } else if (reservationDto.getPreferredRoomType() != null && !reservationDto.getPreferredRoomType().isBlank()) {
                    existingReservation.setPreferredRoomType(reservationDto.getPreferredRoomType());
                    if (existingReservation.getRoom() == null) {
                        room = findAvailableRoomByType(reservationDto.getPreferredRoomType(), reservationDto.getCheckInDate(), reservationDto.getCheckOutDate());
                        if (room != null) {
                            // 标记为 RESERVED 并保存
                            if (room.getStatus() != Room.RoomStatus.AVAILABLE) {
                                throw new RuntimeException("房间当前不可用");
                            }
                            room.setStatus(Room.RoomStatus.RESERVED);
                            roomRepository.save(room);
                            existingReservation.setRoom(room);
                        }
                    }
                }

                existingReservation.setGuest(guest);
                existingReservation.setCheckInDate(reservationDto.getCheckInDate());
                existingReservation.setCheckOutDate(reservationDto.getCheckOutDate());
                existingReservation.setNumberOfGuests(reservationDto.getNumberOfGuests());
                existingReservation.setTotalAmount(reservationDto.getTotalAmount());
                existingReservation.setPaidAmount(reservationDto.getPaidAmount());
                existingReservation.setStatus(reservationDto.getStatus());
                existingReservation.setSpecialRequests(reservationDto.getSpecialRequests());
                existingReservation.setPreferredRoomType(reservationDto.getPreferredRoomType());

                Reservation.ReservationStatus oldStatus = existingReservation.getStatus();
                Reservation updatedReservation = reservationRepository.save(existingReservation);
                
                // 记录更新历史
                if (oldStatus != updatedReservation.getStatus()) {
                    recordHistory(id, "STATUS_CHANGED", oldStatus.toString(), 
                        updatedReservation.getStatus().toString(), "预订状态变更", null);
                } else {
                    recordHistory(id, "MODIFIED", null, null, "更新预订信息", null);
                }
                
                return ReservationDto.fromEntity(updatedReservation);
            } catch (OptimisticLockingFailureException ex) {
                attempts++;
                if (attempts >= MAX_ASSIGN_RETRIES) {
                    throw new RuntimeException("房间分配冲突，稍后重试");
                }
                // 重试循环
            }
        }
    }

    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new RuntimeException("预订不存在");
        }
        reservationRepository.deleteById(id);
    }

    public List<ReservationDto> getReservationsByGuestId(Long guestId) {
        return reservationRepository.findByGuestId(guestId).stream()
                .map(ReservationDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReservationDto> getReservationsByRoomId(Long roomId) {
        return reservationRepository.findByRoomId(roomId).stream()
                .map(ReservationDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReservationDto> getReservationsByStatus(Reservation.ReservationStatus status) {
        return reservationRepository.findByStatus(status).stream()
                .map(ReservationDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReservationDto> getReservationsByCheckInDateRange(LocalDate start, LocalDate end) {
        return reservationRepository.findByCheckInDateBetween(start, end).stream()
                .map(ReservationDto::fromEntity)
                .collect(Collectors.toList());
    }

    private boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return isRoomAvailable(roomId, checkIn, checkOut, null);
    }

    private boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        List<Reservation> conflictingReservations = reservationRepository
                .findConflictingReservations(roomId, checkIn, checkOut, excludeReservationId);
        return conflictingReservations.isEmpty();
    }

    // 按房型查找一个在指定日期范围内可用的房间（优先返回可用状态且无冲突的房间）
    private Room findAvailableRoomByType(String roomType, LocalDate checkIn, LocalDate checkOut) {
        List<Room> candidates = roomRepository.findByRoomType(roomType);
        if (candidates == null || candidates.isEmpty()) return null;
        for (Room r : candidates) {
            if (r.getStatus() != Room.RoomStatus.AVAILABLE) continue;
            if (isRoomAvailable(r.getId(), checkIn, checkOut)) {
                return r;
            }
        }
        return null;
    }

    // 获取取消政策（从系统配置读取）
    private Map<String, BigDecimal> getCancellationPolicy() {
        Optional<SystemSetting> setting = systemSettingRepository.findByKey("cancellation_policy");
        if (setting.isPresent()) {
            try {
                return objectMapper.readValue(setting.get().getValue(), 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, BigDecimal.class));
            } catch (Exception e) {
                // 使用默认政策
            }
        }
        // 默认取消政策：7天前100%，3天前80%，24小时前50%，24小时内10%，当天0%
        Map<String, BigDecimal> defaultPolicy = new HashMap<>();
        defaultPolicy.put("7_days", new BigDecimal("1.00"));
        defaultPolicy.put("3_days", new BigDecimal("0.80"));
        defaultPolicy.put("24_hours", new BigDecimal("0.50"));
        defaultPolicy.put("within_24_hours", new BigDecimal("0.10"));
        defaultPolicy.put("same_day", BigDecimal.ZERO);
        return defaultPolicy;
    }
    
    // 取消预订：创建退款交易（PENDING），返回交易ID供前端调用回调
    @com.hotelsystem.audit.Auditable(action = "CANCEL_RESERVATION")
    public Map<String, Object> cancelReservation(Long reservationId, String cancelReason) {
        Reservation existingReservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        if (existingReservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            throw new RuntimeException("预订已被取消");
        }

        if (existingReservation.getStatus() == Reservation.ReservationStatus.CHECKED_IN ||
                existingReservation.getStatus() == Reservation.ReservationStatus.CHECKED_OUT) {
            throw new RuntimeException("已入住或已离店的预订不能取消");
        }

        // 允许取消的状态：PENDING、CONFIRMED
        // 计算退款金额（使用配置化的取消政策）
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate checkIn = existingReservation.getCheckInDate();
        long daysUntilCheckIn = java.time.temporal.ChronoUnit.DAYS.between(today, checkIn);

        java.math.BigDecimal paid = existingReservation.getPaidAmount() == null ? java.math.BigDecimal.ZERO : existingReservation.getPaidAmount();
        java.math.BigDecimal refund = java.math.BigDecimal.ZERO;
        java.math.BigDecimal refundRate = BigDecimal.ZERO;

        Map<String, BigDecimal> policy = getCancellationPolicy();
        
        if (daysUntilCheckIn >= 7) {
            refundRate = policy.getOrDefault("7_days", new BigDecimal("1.00"));
        } else if (daysUntilCheckIn >= 3) {
            refundRate = policy.getOrDefault("3_days", new BigDecimal("0.80"));
        } else if (daysUntilCheckIn >= 1) {
            refundRate = policy.getOrDefault("24_hours", new BigDecimal("0.50"));
        } else if (daysUntilCheckIn == 0 && today.isBefore(checkIn)) {
            refundRate = policy.getOrDefault("within_24_hours", new BigDecimal("0.10"));
        } else {
            refundRate = policy.getOrDefault("same_day", BigDecimal.ZERO);
        }
        
        refund = paid.multiply(refundRate).setScale(2, RoundingMode.HALF_UP);

        // 创建退款交易，状态为 PENDING
        // 只有在退款回调成功后，预订状态才会真正变为 CANCELLED
        com.hotelsystem.entity.PaymentTransaction refundTx = paymentService.createPendingRefund(
                reservationId,
                refund,
                "取消预订 " + existingReservation.getReservationNumber() + (cancelReason != null ? "，原因：" + cancelReason : "")
        );

        // 记录取消历史
        recordHistory(reservationId, "CANCELLED", existingReservation.getStatus().toString(), 
            "CANCELLED", "取消预订，退款金额：" + refund + "，退款比例：" + refundRate.multiply(new BigDecimal("100")) + "%", 
            cancelReason != null ? cancelReason : "系统");

        // 返回交易信息供前端调用回调接口
        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", refundTx.getId());
        result.put("refundAmount", refund);
        result.put("refundRate", refundRate);
        result.put("reservationId", existingReservation.getId());
        result.put("message", "取消预订成功，请确认退款。请调用 /payments/callback 接口完成退款流程");
        return result;
    }


    // 办理入住：更新预订状态为 CHECKED_IN，更新房间为 OCCUPIED，可收取补交金额
    @com.hotelsystem.audit.Auditable(action = "CHECK_IN")
    public Map<String, Object> checkIn(Long reservationId, java.math.BigDecimal collectAmount, String staffName) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        if (reservation.getStatus() == Reservation.ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("该预订已入住");
        }
        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            throw new RuntimeException("该预订已被取消，无法办理入住");
        }

        Room room = reservation.getRoom();
        if (room == null) {
            throw new RuntimeException("该预订未指定房间，无法办理入住");
        }

        if (room.getStatus() == Room.RoomStatus.OCCUPIED || room.getStatus() == Room.RoomStatus.MAINTENANCE) {
            throw new RuntimeException("房间当前不可入住");
        }

        // 如果需要收取补交金额，则增加到 paidAmount
        java.math.BigDecimal paid = reservation.getPaidAmount() == null ? java.math.BigDecimal.ZERO : reservation.getPaidAmount();
        if (collectAmount != null && collectAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            paid = paid.add(collectAmount);
            reservation.setPaidAmount(paid);
            try {
                paymentService.createPendingPayment(reservation.getId(), collectAmount, "Check-in collect");
            } catch (Exception ex) {
                // ignore payment recording failure
            }
        }

        Reservation.ReservationStatus oldStatus = reservation.getStatus();
        reservation.setStatus(Reservation.ReservationStatus.CHECKED_IN);
        reservation.setCreatedBy(staffName);
        reservationRepository.save(reservation);
        
        // 记录入住历史
        recordHistory(reservationId, "CHECKED_IN", oldStatus.toString(), 
            Reservation.ReservationStatus.CHECKED_IN.toString(), "办理入住", staffName);

        room.setStatus(Room.RoomStatus.OCCUPIED);
        roomRepository.save(room);

        Map<String, Object> result = new HashMap<>();
        result.put("reservationId", reservation.getId());
        result.put("roomNumber", room.getRoomNumber());
        result.put("status", reservation.getStatus());
        result.put("paidAmount", reservation.getPaidAmount());
        return result;
    }

    // 办理退房：结算费用，计算应付/退款，更新房间为 CLEANING，更新预订为 CHECKED_OUT
    @com.hotelsystem.audit.Auditable(action = "CHECK_OUT")
    public Map<String, Object> checkOut(Long reservationId, java.math.BigDecimal extraCharges, java.math.BigDecimal collectAmount, String staffName) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        if (reservation.getStatus() != Reservation.ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("只有已入住的预订才能办理退房");
        }

        Room room = reservation.getRoom();
        if (room == null) {
            throw new RuntimeException("该预订未绑定房间");
        }

        // 计算应付金额：默认使用 reservation.totalAmount，如果为空则按房价×天数计算
        java.math.BigDecimal totalAmount = reservation.getTotalAmount();
        if (totalAmount == null) {
            long days = reservation.getCheckInDate().until(reservation.getCheckOutDate()).getDays();
            if (days <= 0) days = 1;
            java.math.BigDecimal price = room.getPrice() == null ? java.math.BigDecimal.ZERO : room.getPrice();
            totalAmount = price.multiply(java.math.BigDecimal.valueOf(days));
            reservation.setTotalAmount(totalAmount);
        }

        java.math.BigDecimal extras = extraCharges == null ? java.math.BigDecimal.ZERO : extraCharges;
        java.math.BigDecimal paid = reservation.getPaidAmount() == null ? java.math.BigDecimal.ZERO : reservation.getPaidAmount();

        // 如果前台收取补交金额
        if (collectAmount != null && collectAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            paid = paid.add(collectAmount);
            reservation.setPaidAmount(paid);
            try {
                paymentService.createPendingPayment(reservation.getId(), collectAmount, "Check-out collect");
            } catch (Exception ex) {
                // ignore
            }
        }

        // 重新计算退款或待收金额
        java.math.BigDecimal finalDue = totalAmount.add(extras).subtract(paid);
        java.math.BigDecimal refund = java.math.BigDecimal.ZERO;
        if (finalDue.compareTo(java.math.BigDecimal.ZERO) < 0) {
            refund = finalDue.negate();
            finalDue = java.math.BigDecimal.ZERO;
        }

        Reservation.ReservationStatus oldStatus = reservation.getStatus();
        reservation.setStatus(Reservation.ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);
        
        // 记录退房历史
        recordHistory(reservationId, "CHECKED_OUT", oldStatus.toString(), 
            Reservation.ReservationStatus.CHECKED_OUT.toString(), "办理退房，总金额：" + totalAmount + "，退款：" + refund, staffName);

        room.setStatus(Room.RoomStatus.CLEANING);
        roomRepository.save(room);

        // 自动创建清洁任务
        try {
            taskService.createTask(
                    "清洁房间 " + room.getRoomNumber(),
                    "退房后需要清洁房间",
                    Task.TaskType.CLEANING,
                    room.getId(),
                    reservation.getId(),
                    null, // 未分配，等待房务员工认领
                    7, // 中等优先级
                    java.time.LocalDateTime.now().plusHours(2), // 2小时后截止
                    staffName
            );
        } catch (Exception ex) {
            // 任务创建失败不影响退房流程
        }

        Map<String, Object> result = new HashMap<>();
        result.put("reservationId", reservation.getId());
        result.put("totalAmount", totalAmount);
        result.put("extras", extras);
        result.put("paidAmount", paid);
        result.put("amountDue", finalDue);
        result.put("refundAmount", refund);
        result.put("roomNumber", room.getRoomNumber());
        result.put("status", reservation.getStatus());
        return result;
    }

    // 定时任务：回收已过预定入住日期但未办理入住的预订，释放占用的房间并尝试记录退款
    @Scheduled(cron = "0 0 * * * *") // 每小时运行一次
    @Transactional
    public void releaseExpiredReservations() {
        java.time.LocalDate today = java.time.LocalDate.now();
        // 查找状态为 PENDING 或 CONFIRMED，且 checkInDate 在今天之前的预订（已错过入住）
        List<Reservation> expired = reservationRepository.findByStatusInAndCheckInDateBefore(
                Arrays.asList(Reservation.ReservationStatus.PENDING, Reservation.ReservationStatus.CONFIRMED),
                today);

        for (Reservation r : expired) {
            try {
                // 跳过已经办理入住或已取消的
                if (r.getStatus() == Reservation.ReservationStatus.CHECKED_IN ||
                        r.getStatus() == Reservation.ReservationStatus.CHECKED_OUT ||
                        r.getStatus() == Reservation.ReservationStatus.CANCELLED) {
                    continue;
                }

                r.setStatus(Reservation.ReservationStatus.CANCELLED);
                reservationRepository.save(r);

                Room room = r.getRoom();
                if (room != null && room.getStatus() == Room.RoomStatus.RESERVED) {
                    room.setStatus(Room.RoomStatus.AVAILABLE);
                    roomRepository.save(room);
                }

                java.math.BigDecimal paid = r.getPaidAmount() == null ? java.math.BigDecimal.ZERO : r.getPaidAmount();
                if (paid.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    try {
                        paymentService.createPendingRefund(r.getId(), paid, "Auto refund for missed check-in");
                    } catch (Exception ex) {
                        // ignore refund recording failures
                    }
                }
            } catch (Exception ex) {
                // 单个处理失败不影响其他记录；在真实系统中应记录日志或报警
            }
        }
    }
    
    // 执行房间检查
    @Transactional
    public Map<String, Object> performRoomInspection(Long reservationId, CheckOutRequest request, String staffName) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));
        
        Room room = reservation.getRoom();
        if (room == null) {
            throw new RuntimeException("该预订未绑定房间");
        }
        
        // 创建或更新房间检查记录
        RoomCheckInspection inspection = roomCheckInspectionRepository.findByReservationId(reservationId)
                .orElse(new RoomCheckInspection());
        
        inspection.setReservation(reservation);
        inspection.setRoom(room);
        inspection.setFacilitiesOk(request.getHasDamage() == null || !request.getHasDamage());
        inspection.setHasDamage(request.getHasDamage() != null && request.getHasDamage());
        inspection.setDamageDescription(request.getDamageDescription());
        inspection.setItemsLeftBehind(request.getItemsLeftBehind() != null && request.getItemsLeftBehind());
        inspection.setItemsDescription(request.getItemsDescription());
        inspection.setInspectionCompleted(true);
        inspection.setInspector(staffName);
        inspection.setInspectedAt(java.time.LocalDateTime.now());
        
        roomCheckInspectionRepository.save(inspection);
        
        Map<String, Object> result = new HashMap<>();
        result.put("inspectionId", inspection.getId());
        result.put("completed", true);
        return result;
    }
    
    // 获取退房账单详情
    public Map<String, Object> getCheckOutBillDetails(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));
        
        Map<String, Object> billDetails = new HashMap<>();
        
        // 房费明细
        Map<String, Object> roomCharges = new HashMap<>();
        long days = reservation.getCheckInDate().until(reservation.getCheckOutDate()).getDays();
        if (days <= 0) days = 1;
        roomCharges.put("days", days);
        roomCharges.put("roomPrice", reservation.getRoom() != null ? reservation.getRoom().getPrice() : BigDecimal.ZERO);
        roomCharges.put("subtotal", reservation.getTotalAmount() != null ? reservation.getTotalAmount() : BigDecimal.ZERO);
        billDetails.put("roomCharges", roomCharges);
        
        // POS消费明细
        List<com.hotelsystem.entity.PosConsumption> posConsumptions = posConsumptionRepository.findByReservationId(reservationId);
        List<Map<String, Object>> posDetails = new java.util.ArrayList<>();
        BigDecimal totalPosAmount = BigDecimal.ZERO;
        for (var pos : posConsumptions) {
            Map<String, Object> posItem = new HashMap<>();
            posItem.put("item", pos.getItemName());
            posItem.put("category", pos.getCategory());
            posItem.put("quantity", pos.getQuantity());
            posItem.put("unitPrice", pos.getUnitPrice());
            posItem.put("amount", pos.getTotalAmount());
            posItem.put("consumedAt", pos.getConsumptionDate());
            posDetails.add(posItem);
            totalPosAmount = totalPosAmount.add(pos.getTotalAmount() != null ? pos.getTotalAmount() : BigDecimal.ZERO);
        }
        billDetails.put("posConsumptions", posDetails);
        billDetails.put("totalPosAmount", totalPosAmount);
        
        // 杂费（从extraCharges获取，这里简化处理）
        billDetails.put("miscCharges", BigDecimal.ZERO);
        
        // 已付金额
        billDetails.put("paidAmount", reservation.getPaidAmount() != null ? reservation.getPaidAmount() : BigDecimal.ZERO);
        
        // 计算应付金额
        BigDecimal totalAmount = reservation.getTotalAmount() != null ? reservation.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.add(totalPosAmount);
        BigDecimal paid = reservation.getPaidAmount() != null ? reservation.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal amountDue = finalAmount.subtract(paid);
        BigDecimal refund = BigDecimal.ZERO;
        if (amountDue.compareTo(BigDecimal.ZERO) < 0) {
            refund = amountDue.negate();
            amountDue = BigDecimal.ZERO;
        }
        
        billDetails.put("totalAmount", finalAmount);
        billDetails.put("amountDue", amountDue);
        billDetails.put("refundAmount", refund);
        
        // 支付记录
        List<com.hotelsystem.entity.PaymentTransaction> payments = paymentTransactionRepository.findByReservationId(reservationId);
        billDetails.put("paymentHistory", payments);
        
        return billDetails;
    }
    
    /**
     * 改订预订：支持修改日期和房间，计算差价
     */
    @Transactional
    public Map<String, Object> modifyReservation(Long reservationId, LocalDate newCheckInDate, 
                                                  LocalDate newCheckOutDate, Long newRoomId, 
                                                  String newPreferredRoomType, String operator) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        if (reservation.getStatus() == Reservation.ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == Reservation.ReservationStatus.CHECKED_OUT ||
                reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            throw new RuntimeException("已入住、已离店或已取消的预订不能改订");
        }

        // 保存原值用于对比
        LocalDate oldCheckInDate = reservation.getCheckInDate();
        LocalDate oldCheckOutDate = reservation.getCheckOutDate();
        Room oldRoom = reservation.getRoom();
        BigDecimal oldTotalAmount = reservation.getTotalAmount();

        // 更新日期
        if (newCheckInDate != null) {
            reservation.setCheckInDate(newCheckInDate);
        }
        if (newCheckOutDate != null) {
            reservation.setCheckOutDate(newCheckOutDate);
        }

        // 更新房间
        Room newRoom = null;
        if (newRoomId != null) {
            newRoom = roomRepository.findById(newRoomId)
                    .orElseThrow(() -> new RuntimeException("新房间不存在"));
            if (!isRoomAvailable(newRoomId, 
                    newCheckInDate != null ? newCheckInDate : reservation.getCheckInDate(),
                    newCheckOutDate != null ? newCheckOutDate : reservation.getCheckOutDate(),
                    reservationId)) {
                throw new RuntimeException("新房间在指定日期不可用");
            }
            reservation.setRoom(newRoom);
        } else if (newPreferredRoomType != null && !newPreferredRoomType.isBlank()) {
            newRoom = findAvailableRoomByType(newPreferredRoomType,
                    newCheckInDate != null ? newCheckInDate : reservation.getCheckInDate(),
                    newCheckOutDate != null ? newCheckOutDate : reservation.getCheckOutDate());
            if (newRoom == null) {
                throw new RuntimeException("未找到可用的" + newPreferredRoomType + "房间");
            }
            reservation.setRoom(newRoom);
            reservation.setPreferredRoomType(newPreferredRoomType);
        }

        // 计算新总金额
        Room roomForPrice = newRoom != null ? newRoom : (reservation.getRoom() != null ? reservation.getRoom() : oldRoom);
        if (roomForPrice == null) {
            throw new RuntimeException("无法确定房间价格");
        }

        LocalDate checkIn = newCheckInDate != null ? newCheckInDate : reservation.getCheckInDate();
        LocalDate checkOut = newCheckOutDate != null ? newCheckOutDate : reservation.getCheckOutDate();
        long days = checkIn.until(checkOut).getDays();
        if (days <= 0) {
            throw new RuntimeException("离店日期必须晚于入住日期");
        }

        BigDecimal newTotalAmount = roomForPrice.getPrice().multiply(BigDecimal.valueOf(days));
        reservation.setTotalAmount(newTotalAmount);

        // 计算差价
        BigDecimal priceDifference = newTotalAmount.subtract(oldTotalAmount != null ? oldTotalAmount : BigDecimal.ZERO);
        BigDecimal paidAmount = reservation.getPaidAmount() != null ? reservation.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal amountDue = priceDifference.subtract(paidAmount);
        BigDecimal refundAmount = BigDecimal.ZERO;
        if (amountDue.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = amountDue.negate();
            amountDue = BigDecimal.ZERO;
        }

        reservationRepository.save(reservation);

        // 记录改订历史
        StringBuilder changeDesc = new StringBuilder("改订预订：");
        if (!oldCheckInDate.equals(reservation.getCheckInDate()) || 
            !oldCheckOutDate.equals(reservation.getCheckOutDate())) {
            changeDesc.append("日期变更 ");
        }
        if (oldRoom != null && !oldRoom.getId().equals(reservation.getRoom().getId())) {
            changeDesc.append("房间变更 ");
        }
        changeDesc.append("差价：").append(priceDifference);
        recordHistory(reservationId, "MODIFIED", oldTotalAmount.toString(), 
            newTotalAmount.toString(), changeDesc.toString(), operator);

        Map<String, Object> result = new HashMap<>();
        result.put("reservation", ReservationDto.fromEntity(reservation));
        result.put("oldTotalAmount", oldTotalAmount);
        result.put("newTotalAmount", newTotalAmount);
        result.put("priceDifference", priceDifference);
        result.put("amountDue", amountDue);
        result.put("refundAmount", refundAmount);
        result.put("message", priceDifference.compareTo(BigDecimal.ZERO) > 0 ? 
            "需要补交 " + amountDue + " 元" : 
            "将退还 " + refundAmount + " 元");

        return result;
    }
    
    /**
     * 获取可用房间列表（用于改订界面）
     */
    public List<Map<String, Object>> getAvailableRoomsForModify(LocalDate checkInDate, LocalDate checkOutDate, 
                                                                 String preferredRoomType) {
        List<Room> allRooms = roomRepository.findAll();
        List<Map<String, Object>> availableRooms = new java.util.ArrayList<>();

        for (Room room : allRooms) {
            // 过滤房型
            if (preferredRoomType != null && !preferredRoomType.isBlank() && 
                !room.getRoomType().equals(preferredRoomType)) {
                continue;
            }

            // 检查可用性
            if (isRoomAvailable(room.getId(), checkInDate, checkOutDate)) {
                Map<String, Object> roomInfo = new HashMap<>();
                roomInfo.put("id", room.getId());
                roomInfo.put("roomNumber", room.getRoomNumber());
                roomInfo.put("roomType", room.getRoomType());
                roomInfo.put("price", room.getPrice());
                roomInfo.put("capacity", room.getCapacity());
                roomInfo.put("amenities", room.getAmenities());
                roomInfo.put("status", room.getStatus());
                
                // 计算总价
                long days = checkInDate.until(checkOutDate).getDays();
                roomInfo.put("totalPrice", room.getPrice().multiply(BigDecimal.valueOf(days)));
                
                availableRooms.add(roomInfo);
            }
        }

        return availableRooms;
    }
}