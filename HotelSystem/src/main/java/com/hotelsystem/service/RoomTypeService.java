package com.hotelsystem.service;

import com.hotelsystem.entity.RoomType;
import com.hotelsystem.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public List<RoomType> getActiveRoomTypes() {
        return roomTypeRepository.findByIsActiveTrue();
    }

    public Optional<RoomType> getRoomTypeById(Long id) {
        return roomTypeRepository.findById(id);
    }

    public Optional<RoomType> getRoomTypeByName(String name) {
        return roomTypeRepository.findByName(name);
    }

    public RoomType createRoomType(RoomType roomType) {
        if (roomTypeRepository.findByName(roomType.getName()).isPresent()) {
            throw new RuntimeException("房型名称已存在");
        }
        return roomTypeRepository.save(roomType);
    }

    public RoomType updateRoomType(Long id, RoomType roomType) {
        RoomType existing = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房型不存在"));

        // 检查名称是否被其他房型使用
        Optional<RoomType> existingByName = roomTypeRepository.findByName(roomType.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new RuntimeException("房型名称已被其他房型使用");
        }

        existing.setName(roomType.getName());
        existing.setDescription(roomType.getDescription());
        existing.setBasePrice(roomType.getBasePrice());
        existing.setCapacity(roomType.getCapacity());
        existing.setAmenities(roomType.getAmenities());
        existing.setPhotos(roomType.getPhotos());
        existing.setIsActive(roomType.getIsActive());

        return roomTypeRepository.save(existing);
    }

    public void deleteRoomType(Long id) {
        if (!roomTypeRepository.existsById(id)) {
            throw new RuntimeException("房型不存在");
        }
        roomTypeRepository.deleteById(id);
    }
}

