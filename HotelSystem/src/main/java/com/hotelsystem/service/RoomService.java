package com.hotelsystem.service;

import com.hotelsystem.dto.RoomDto;
import com.hotelsystem.entity.Reservation;
import com.hotelsystem.entity.Room;
import com.hotelsystem.repository.ReservationRepository;
import com.hotelsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public List<RoomDto> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(RoomDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<RoomDto> getRoomById(Long id) {
        return roomRepository.findById(id)
                .map(RoomDto::fromEntity);
    }

    public RoomDto createRoom(RoomDto roomDto) {
        if (roomRepository.existsByRoomNumber(roomDto.getRoomNumber())) {
            throw new RuntimeException("房间号已存在");
        }
        Room room = roomDto.toEntity();
        Room savedRoom = roomRepository.save(room);
        return RoomDto.fromEntity(savedRoom);
    }

    public RoomDto updateRoom(Long id, RoomDto roomDto) {
        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        if (!existingRoom.getRoomNumber().equals(roomDto.getRoomNumber()) &&
                roomRepository.existsByRoomNumber(roomDto.getRoomNumber())) {
            throw new RuntimeException("房间号已被其他房间使用");
        }

        existingRoom.setRoomNumber(roomDto.getRoomNumber());
        existingRoom.setRoomType(roomDto.getRoomType());
        existingRoom.setDescription(roomDto.getDescription());
        existingRoom.setPrice(roomDto.getPrice());
        existingRoom.setCapacity(roomDto.getCapacity());
        existingRoom.setAmenities(roomDto.getAmenities());
        existingRoom.setStatus(roomDto.getStatus());
        existingRoom.setIsActive(roomDto.getIsActive());

        Room updatedRoom = roomRepository.save(existingRoom);
        return RoomDto.fromEntity(updatedRoom);
    }

    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("房间不存在");
        }
        roomRepository.deleteById(id);
    }

    /**
     * 获取在给定日期范围内可用的房间。如果不提供日期参数，则默认检查当天是否有冲突的预订。
     */
    public List<RoomDto> getAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        LocalDate start = (checkIn == null) ? LocalDate.now() : checkIn;
        LocalDate end = (checkOut == null) ? start.plusDays(1) : checkOut;

        return roomRepository.findAll().stream()
                .filter(room -> {
                    if (room.getStatus() == Room.RoomStatus.MAINTENANCE || room.getStatus() == Room.RoomStatus.OCCUPIED) {
                        return false;
                    }
                    // 查询是否有已确认或已入住的预订与候选时间段冲突
                    List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                            room.getId(), start, end, null);
                    return conflicts.isEmpty();
                })
                .map(RoomDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 兼容旧版本调用：不传参时返回当天可用房间
    public List<RoomDto> getAvailableRooms() {
        return getAvailableRooms(null, null);
    }

    public List<RoomDto> getRoomsByType(String roomType) {
        return roomRepository.findByRoomType(roomType).stream()
                .map(RoomDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<RoomDto> getActiveRooms() {
        return roomRepository.findByIsActiveTrue().stream()
                .map(RoomDto::fromEntity)
                .collect(Collectors.toList());
    }
}