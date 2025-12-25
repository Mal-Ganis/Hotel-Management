package com.hotelsystem.repository;

import com.hotelsystem.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findByStatus(Room.RoomStatus status);
    List<Room> findByRoomType(String roomType);
    List<Room> findByIsActiveTrue();
    Boolean existsByRoomNumber(String roomNumber);
}