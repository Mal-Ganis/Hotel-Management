package com.hotelsystem.service;

import com.hotelsystem.entity.Waitlist;
import com.hotelsystem.entity.Guest;
import com.hotelsystem.entity.Reservation;
import com.hotelsystem.entity.Room;
import com.hotelsystem.repository.WaitlistRepository;
import com.hotelsystem.repository.GuestRepository;
import com.hotelsystem.repository.ReservationRepository;
import com.hotelsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final ReservationService reservationService;

    public List<Waitlist> getAllWaitlist() {
        return waitlistRepository.findAll();
    }

    public List<Waitlist> getPendingWaitlist() {
        return waitlistRepository.findByStatus(Waitlist.WaitlistStatus.PENDING);
    }

    public Optional<Waitlist> getWaitlistById(Long id) {
        return waitlistRepository.findById(id);
    }

    @Transactional
    public Waitlist addToWaitlist(Long guestId, LocalDate desiredCheckInDate, LocalDate desiredCheckOutDate, 
                                  String preferredRoomType, String phone, String email) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("宾客不存在"));

        Waitlist waitlist = new Waitlist();
        waitlist.setGuest(guest);
        waitlist.setDesiredCheckInDate(desiredCheckInDate);
        waitlist.setDesiredCheckOutDate(desiredCheckOutDate);
        waitlist.setPreferredRoomType(preferredRoomType);
        waitlist.setPhone(phone);
        waitlist.setEmail(email);
        waitlist.setStatus(Waitlist.WaitlistStatus.PENDING);

        return waitlistRepository.save(waitlist);
    }

    @Transactional
    public void notifyWaitlist(Long waitlistId) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new RuntimeException("候补记录不存在"));

        waitlist.setStatus(Waitlist.WaitlistStatus.NOTIFIED);
        waitlist.setNotifiedAt(java.time.LocalDateTime.now());
        waitlistRepository.save(waitlist);
    }

    @Transactional
    public Reservation convertToReservation(Long waitlistId, Long roomId) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new RuntimeException("候补记录不存在"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        // 检查房间是否可用
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                roomId, waitlist.getDesiredCheckInDate(), waitlist.getDesiredCheckOutDate(), null);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("房间在指定日期不可用");
        }

        // 创建预订
        com.hotelsystem.dto.ReservationDto reservationDto = new com.hotelsystem.dto.ReservationDto();
        reservationDto.setGuestId(waitlist.getGuest().getId());
        reservationDto.setRoomId(roomId);
        reservationDto.setCheckInDate(waitlist.getDesiredCheckInDate());
        reservationDto.setCheckOutDate(waitlist.getDesiredCheckOutDate());
        reservationDto.setPreferredRoomType(waitlist.getPreferredRoomType());

        com.hotelsystem.dto.ReservationDto createdReservation = reservationService.createReservation(reservationDto);

        // 更新候补状态
        waitlist.setStatus(Waitlist.WaitlistStatus.CONVERTED);
        waitlistRepository.save(waitlist);

        return reservationRepository.findById(createdReservation.getId())
                .orElseThrow(() -> new RuntimeException("预订创建失败"));
    }

    @Transactional
    public void cancelWaitlist(Long waitlistId) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new RuntimeException("候补记录不存在"));

        waitlist.setStatus(Waitlist.WaitlistStatus.CANCELLED);
        waitlistRepository.save(waitlist);
    }

    // 检查是否有可用房间并自动通知候补客户
    @Transactional
    public void checkAndNotifyAvailableRooms(LocalDate checkInDate, LocalDate checkOutDate, String roomType) {
        List<Waitlist> pendingWaitlist = waitlistRepository.findByDesiredCheckInDateBetweenAndStatus(
                checkInDate, checkOutDate, Waitlist.WaitlistStatus.PENDING);

        for (Waitlist waitlist : pendingWaitlist) {
            if (roomType == null || roomType.equals(waitlist.getPreferredRoomType())) {
                // 检查是否有可用房间
                List<Room> availableRooms = roomRepository.findByRoomType(
                        waitlist.getPreferredRoomType() != null ? waitlist.getPreferredRoomType() : roomType);
                
                for (Room availableRoom : availableRooms) {
                    if (availableRoom.getStatus() == Room.RoomStatus.AVAILABLE) {
                        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                                availableRoom.getId(), waitlist.getDesiredCheckInDate(), waitlist.getDesiredCheckOutDate(), null);
                        if (conflicts.isEmpty()) {
                            // 找到可用房间，通知候补客户
                            notifyWaitlist(waitlist.getId());
                            break;
                        }
                    }
                }
            }
        }
    }
}

