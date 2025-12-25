package com.hotelsystem.dto;

import com.hotelsystem.entity.Room;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RoomDto {
    private Long id;

    @NotBlank(message = "房间号不能为空")
    @Size(max = 20, message = "房间号长度不能超过20个字符")
    private String roomNumber;

    @NotBlank(message = "房型不能为空")
    @Size(max = 50, message = "房型长度不能超过50个字符")
    private String roomType;

    @Size(max = 200, message = "描述长度不能超过200个字符")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;

    @NotNull(message = "容纳人数不能为空")
    private Integer capacity;

    @Size(max = 100, message = "设施长度不能超过100个字符")
    private String amenities;

    private Room.RoomStatus status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RoomDto fromEntity(Room room) {
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setRoomType(room.getRoomType());
        dto.setDescription(room.getDescription());
        dto.setPrice(room.getPrice());
        dto.setCapacity(room.getCapacity());
        dto.setAmenities(room.getAmenities());
        dto.setStatus(room.getStatus());
        dto.setIsActive(room.getIsActive());
        dto.setCreatedAt(room.getCreatedAt());
        dto.setUpdatedAt(room.getUpdatedAt());
        return dto;
    }

    public Room toEntity() {
        Room room = new Room();
        room.setRoomNumber(this.roomNumber);
        room.setRoomType(this.roomType);
        room.setDescription(this.description);
        room.setPrice(this.price);
        room.setCapacity(this.capacity);
        room.setAmenities(this.amenities);
        room.setStatus(this.status != null ? this.status : Room.RoomStatus.AVAILABLE);
        room.setIsActive(this.isActive != null ? this.isActive : true);
        return room;
    }
}